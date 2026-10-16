package org.bread_experts_group.bread_systems_library.ffm

import jdk.dynalink.linker.support.Guards
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.ERRNO
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.LAST_ERROR
import org.bread_experts_group.bread_systems_library.ffm.c.PreserveErrno
import org.bread_experts_group.bread_systems_library.ffm.c.types.*
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.windows.PreserveLastError
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.AccessFlag
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.*
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

interface LibrarySource {
    val linker: Linker
}

fun KType.memoryLayout(
    linker: Linker,
    padEnd: Boolean = true,
    unaligned: Boolean = false
): MemoryLayout = when (val clazz = this.classifier!! as KClass<*>) {
    Pointer::class, MemorySegment::class -> return if (unaligned) ValueLayout.ADDRESS_UNALIGNED else ValueLayout.ADDRESS

    unsigned__int64::class, Long::class -> return if (unaligned) ValueLayout.JAVA_LONG_UNALIGNED else ValueLayout.JAVA_LONG
    long::class, unsigned_long::class -> linker.canonicalLayouts()["long"]!!

    Int::class -> return if (unaligned) ValueLayout.JAVA_INT_UNALIGNED else ValueLayout.JAVA_INT
    int::class, unsigned_int::class -> linker.canonicalLayouts()["int"]!!

    Short::class -> return if (unaligned) ValueLayout.JAVA_SHORT_UNALIGNED else ValueLayout.JAVA_SHORT
    short::class, unsigned_short::class -> linker.canonicalLayouts()["short"]!!

    char::class, unsigned_char::class -> linker.canonicalLayouts()["char"]!!

    Byte::class -> ValueLayout.JAVA_BYTE
    size_t::class, ssize_t::class -> linker.canonicalLayouts()["size_t"]!!

    else -> {
        val companion = clazz.companionObjectInstance
        if (companion is TypeDynamicallySolved) return companion.solve().memoryLayout(linker, padEnd, unaligned)

        if (Structure::class.isSuperclassOf(clazz)) return this.computeLayout(linker, padEnd, unaligned)

        val superType = clazz.supertypes.first { !(it.classifier!! as KClass<*>).java.isInterface }
        if (superType.classifier == Any::class) throw NotImplementedError("Edge: $this")
        superType.memoryLayout(linker, padEnd, unaligned)
    }
}.let {
    if (unaligned) it.withByteAlignment(1) else it
}

fun Method.type(): MethodType = MethodType.methodType(
    this.returnType,
    this.parameterTypes
)

fun Method.handle(lookup: MethodHandles.Lookup): MethodHandle {
    if (this.modifiers and Modifier.STATIC != 0) TODO("Static function call")

    return lookup.findVirtual(
        this.declaringClass,
        this.name, this.type()
    )
}

fun KFunction<*>.handle(lookup: MethodHandles.Lookup): MethodHandle = this.javaMethod!!.handle(lookup)

fun KProperty<*>.handle(lookup: MethodHandles.Lookup): MethodHandle {
    this.javaGetter?.handle(lookup)?.let { return it }
    TODO("Field")
}

object NativeMethodFilters {
    fun fixSegment(
        segment: MemorySegment, provider: KType
    ): Any = Pointer<Any>(provider, segment)
}

private fun fixMethodHandle(
    lookup: MethodHandles.Lookup,
    methodHandle: MethodHandle,
    returnType: KType, parameters: List<KTypeProjection>
): MethodHandle {
    var methodHandle = methodHandle
    parameters.forEachIndexed { index, parameter ->
        val classifier = parameter.type!!.classifier as KClass<*>
        val innerType = methodHandle.type().parameterType(index)
        methodHandle = MethodHandles.filterArguments(
            methodHandle,
            index,
            (if (ValueProvider::class.isSuperclassOf(classifier)) ValueProvider::value.handle(lookup)
            else if (Structure::class.isSuperclassOf(classifier)) Structure::segment.handle(lookup)
            else if (Pointer::class.isSuperclassOf(classifier)) MethodHandles.guardWithTest(
                Guards.isNotNull().asType(
                    MethodType.methodType(Boolean::class.java, Pointer::class.java)
                ),
                Pointer<*>::segment.handle(lookup),
                MethodHandles.dropArguments(
                    MethodHandles.constant(MemorySegment::class.java, MemorySegment.NULL),
                    0,
                    Pointer::class.java
                )
            ) else throw NotImplementedError("$classifier"))
                .asType(MethodType.methodType(innerType, classifier.java))
        )
    }

    val returnClass = returnType.classifier as KClass<*>
    if (returnClass == Unit::class || returnClass == Nothing::class) return methodHandle

    val innerType = methodHandle.type().returnType()
    val fixedType = MethodType.methodType(returnClass.java, innerType)
    methodHandle = MethodHandles.filterReturnValue(
        methodHandle,
        if (Pointer::class.isSuperclassOf(returnClass)) MethodHandles.insertArguments(
                NativeMethodFilters::fixSegment.handle(lookup).bindTo(NativeMethodFilters),
                1, returnType.arguments.first().type!!)
            .asType(fixedType)
        else if (ValueProvider::class.isSuperclassOf(returnClass))
            lookup.findConstructor(
                returnClass.java,
                MethodType.methodType(Void.TYPE, Any::class.java)
            )
            .asType(fixedType)
        else throw NotImplementedError("$returnClass")
    )
    return methodHandle
}

private fun createKFBridge(
    lookup: MethodHandles.Lookup,
    parameters: Int, voidReturn: Boolean, methodHandle: MethodHandle
): Any {
    return run {
        val hcd = ClassDesc.of("org.bread_experts_group.bread_systems_library.ffm.AdapterClass")
        lookup.defineHiddenClass(
            ClassFile.of().build(hcd) { builder ->
                builder.withInterfaceSymbols(
                    ClassDesc.of("kotlin.jvm.functions.Function${parameters}")
                )

                builder.withField(
                    "methodHandle", ConstantDescs.CD_MethodHandle,
                    AccessFlag.PRIVATE.mask() or AccessFlag.FINAL.mask()
                )

                builder.withMethodBody(
                    "invoke", MethodTypeDesc.of(
                        ConstantDescs.CD_Object,
                        List(parameters) { ConstantDescs.CD_Object }
                    ),
                    AccessFlag.PUBLIC.mask()
                ) { codeBuilder ->
                    codeBuilder
                        .aload(0)
                        .getfield(hcd, "methodHandle", ConstantDescs.CD_MethodHandle)
                    val type = methodHandle.type()
                    repeat(parameters) {
                        when (val desc = type.parameterType(it).describeConstable().get()) {
                            ConstantDescs.CD_int, ConstantDescs.CD_short, ConstantDescs.CD_byte -> codeBuilder.iload(it + 1)
                            ConstantDescs.CD_float -> codeBuilder.iload(it + 1)
                            else if desc.isClassOrInterface -> codeBuilder.aload(it + 1).checkcast(desc)
                            else -> TODO("${desc.descriptorString()}")
                        }
                    }
                    codeBuilder.invokevirtual(
                        ConstantDescs.CD_MethodHandle, "invoke",
                        type.describeConstable().get()
                    )
                    if (voidReturn) codeBuilder.getstatic(
                        ClassDesc.of(Unit::class.qualifiedName),
                        "INSTANCE",
                        ClassDesc.of(Unit::class.qualifiedName)
                    )
                    codeBuilder.areturn()
                }

                builder.withMethodBody(
                    "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_MethodHandle),
                    AccessFlag.PUBLIC.mask()
                ) { codeBuilder ->
                    codeBuilder
                        .aload(0)
                        .dup()
                        .invokespecial(
                            ConstantDescs.CD_Object, "<init>",
                            MethodTypeDesc.of(ConstantDescs.CD_void)
                        )
                        .aload(1)
                        .putfield(hcd, "methodHandle", ConstantDescs.CD_MethodHandle)
                        .return_()
                }
            },
            true
        ).lookupClass().getConstructor(MethodHandle::class.java)
    }.newInstance(methodHandle)
}

fun get(
    type: KType,
    lookup: SymbolLookup,
    linker: Linker
): Any {
    val constructor = (type.classifier as KClass<*>).constructors.first()
    val arguments = arrayOfNulls<Any>(constructor.parameters.size)
    constructor.parameters.forEach { parameter ->
        if (parameter.type.classifier == Linker::class) {
            arguments[parameter.index] = linker
            return@forEach
        }

        val procedure = lookup.find(parameter.name).getOrNull() ?: return@forEach

        val classifier = parameter.type.classifier as KClass<*>
        val parameterArguments = parameter.type.arguments

        val lookup = MethodHandles.lookup()
        if (Function::class.isSuperclassOf(classifier)) {
            val parameters = parameterArguments.subList(0, parameterArguments.lastIndex)
            val parametersArray = parameters.map { parameter ->
                parameter.type!!.memoryLayout(linker, false)
            }.toTypedArray()

            val returnType = parameterArguments.last().type!!
            val returnClass = returnType.classifier as KClass<*>

            var ple = false
            val options = mutableListOf<Linker.Option>()
            for (annotation in parameter.type.annotations) when (annotation) {
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

            if (returnClass == Unit::class || returnClass == Nothing::class) {
                var methodHandle = linker.downcallHandle(
                    procedure,
                    FunctionDescriptor.ofVoid(*parametersArray),
                    *options.toTypedArray()
                )
                if (ple) methodHandle = MethodHandles.insertArguments(methodHandle, 0, StateLayout.local.get())
                methodHandle = fixMethodHandle(lookup, methodHandle, returnType, parameters)

                arguments[parameter.index] = createKFBridge(lookup, parameterArguments.size - 1, true, methodHandle)
            } else {
                var methodHandle = linker.downcallHandle(
                    procedure,
                    FunctionDescriptor.of(
                        returnType.memoryLayout(linker, false),
                        *parametersArray
                    ),
                    *options.toTypedArray()
                )
                if (ple) methodHandle = MethodHandles.insertArguments(methodHandle, 0, StateLayout.local.get())
                methodHandle = fixMethodHandle(lookup, methodHandle, returnType, parameters)
                arguments[parameter.index] = createKFBridge(lookup, parameterArguments.size - 1, false, methodHandle)
            }
        } else throw IllegalArgumentException("Unable to translate (unknown): $parameter")
    }
    return constructor.call(*arguments)
}

inline fun <reified L: LibrarySource> get(
    lookup: SymbolLookup,
    linker: Linker
): L = get(typeOf<L>(), lookup, linker) as L