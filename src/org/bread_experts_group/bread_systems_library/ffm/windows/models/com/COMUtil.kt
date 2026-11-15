package org.bread_experts_group.bread_systems_library.ffm.windows.models.com

import org.bread_experts_group.bread_systems_library.ffm.*
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.ERRNO
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.LAST_ERROR
import org.bread_experts_group.bread_systems_library.ffm.c.PreserveErrno
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.PreserveLastError
import org.bread_experts_group.bread_systems_library.generic.Order
import org.bread_experts_group.bread_systems_library.generic.desc
import org.bread_experts_group.bread_systems_library.generic.implementsBody
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.AccessFlag
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

private val classFile = ClassFile.of()

fun interpretCOM(linker: Linker, type: KType, ptr: MemorySegment): Any {
    val vTable = ptr[ValueLayout.ADDRESS, 0].reinterpret(Long.MAX_VALUE)
    val parents = buildList {
        var clazz = type.classifier as KClass<*>
        add(clazz)
        while (clazz != IUnknown::class) {
            clazz = clazz.java.superclass.kotlin
            add(clazz)
        }
    }.asReversed()

    val associations = mutableMapOf<Long, KFunction<*>>()
    var initial = 0L
    parents.forEach { clazz ->
        var localHighest = 0L
        clazz.declaredMemberFunctions.forEach { function ->
            val orderAnnotation = function.findAnnotation<Order>() ?: return@forEach
            if ((orderAnnotation.order + 1) > localHighest) localHighest = (orderAnnotation.order + 1)
            associations[initial + orderAnnotation.order] = function
        }
        initial += localHighest
    }
    val parentConstable = (type.classifier as KClass<*>).desc
    val lookup = MethodHandles.lookup()
    val thisDesc = ClassDesc.of("${lookup.lookupClass().packageName.let { if (it.isNotBlank()) "$it." else "" }}COMImplementationClass")
    val hidden = lookup.defineHiddenClass(
        classFile.build(thisDesc) { classBuilder ->
            classBuilder.withSuperclass(parentConstable)
            classBuilder.withField(
                "segment", desc<MemorySegment>(),
                AccessFlag.FINAL.mask() or AccessFlag.PRIVATE.mask()
            )

            classBuilder.withMethodBody(
                "<init>", MethodTypeDesc.of(
                    ConstantDescs.CD_void,
                    desc<MemorySegment>(), desc<MethodHandle>().arrayType()
                ),
                AccessFlag.PUBLIC.mask()
            ) { codeBuilder ->
                codeBuilder
                    .aload(0)
                    .dup()
                    .invokespecial(
                        parentConstable,
                        "<init>", MethodTypeDesc.of(ConstantDescs.CD_void)
                    )
                    .aload(1)
                    .putfield(thisDesc, "segment", desc<MemorySegment>())
                associations.keys.forEachIndexed { index, vTableIndex ->
                    classBuilder.withField(
                        vTableIndex.toString(), desc<MethodHandle>(),
                        AccessFlag.PRIVATE.mask() or AccessFlag.FINAL.mask()
                    )
                    codeBuilder
                        .aload(0)
                        .aload(2)
                        .loadConstant(index)
                        .aaload()
                        .putfield(thisDesc, vTableIndex.toString(), desc<MethodHandle>())

                    val javaFunction = associations[vTableIndex]!!.javaMethod!!
                    classBuilder.implementsBody(javaFunction) {
                        aload(0)
                        getfield(thisDesc, vTableIndex.toString(), desc<MethodHandle>())
                        for (i in 1..javaFunction.parameterCount) {
                            if (javaFunction.parameterTypes[i - 1].isPrimitive) TODO("Temp")
                            aload(i)
                        }
                        invokevirtual(desc<MethodHandle>(), "invoke", javaFunction.desc)
                        if (javaFunction.returnType == Void.TYPE || javaFunction.returnType.isPrimitive) return_()
                        else areturn()
                    }
                }

                codeBuilder.return_()
            }

            classBuilder.implementsBody(IUnknown::segment.javaGetter!!) {
                aload(0)
                getfield(thisDesc, "segment", desc<MemorySegment>())
                areturn()
            }
        },
        true
    )

    val handles = associations.map { (vTableIndex, function) ->
        val functionPtr = vTable.getAtIndex(ValueLayout.ADDRESS, vTableIndex)

        var ple = false
        val options = mutableListOf<Linker.Option>()
        for (annotation in function.annotations) when (annotation) {
            is Critical -> options.add(Linker.Option.critical(annotation.heapAccess))
            is PreserveLastError -> {
                options.add(Linker.Option.captureCallState(LAST_ERROR))
                ple = true
            }
            is PreserveErrno -> {
                options.add(Linker.Option.captureCallState(ERRNO))
                ple = true
            }
        }

        val returnClass = function.returnType.classifier as KClass<*>
        val parameters = function.parameters
            .takeLast(function.parameters.size - 1)
            .map { it.type.memoryLayout(linker, false) }.toTypedArray()
            .toMutableList()
            .apply { addFirst(ValueLayout.ADDRESS) }
            .toTypedArray()
        var methodHandle = linker.downcallHandle(
            functionPtr,
            if (returnClass == Unit::class || returnClass == Nothing::class) FunctionDescriptor.ofVoid(
                *parameters
            ) else FunctionDescriptor.of(
                function.returnType.memoryLayout(linker, false),
                *parameters
            ),
            *options.toTypedArray()
        ).bindTo(ptr)
        if (ple) methodHandle = MethodHandles.insertArguments(methodHandle, 0, StateLayout.local.get())
        methodHandleJVMToNative(
            linker,
            lookup,
            methodHandle,
            function.returnType,
            function.parameters
                .takeLast(function.parameters.size - 1)
                .map { KTypeProjection(KVariance.INVARIANT, it.type) }
        )
    }.toTypedArray()

    return hidden.lookupClass().getConstructor(
        MemorySegment::class.java,
        MethodHandle::class.java.arrayType()
    ).newInstance(ptr, handles)
}

inline fun <reified T : IUnknown> interpretCOM(linker: Linker, ptr: MemorySegment): T = interpretCOM(linker, typeOf<T>(), ptr) as T
inline fun <reified T : IUnknown> interpretCOM(linker: Linker, structure: Structure): T = interpretCOM(linker, structure.segment())
inline fun <reified T : IUnknown> interpretCOM(linker: Linker, ptr: Pointer<T>): T = interpretCOM(linker, typeOf<T>(), ptr.value.segment) as T