package org.bread_experts_group.bread_systems_library.ffm

import org.bread_experts_group.bread_systems_library.ffm.StateLayout.LAST_ERROR
import org.bread_experts_group.bread_systems_library.ffm.c.types.*
import org.bread_experts_group.bread_systems_library.ffm.windows.PreserveLastError
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod

interface LibrarySource

fun KClass<*>.memoryLayout(linker: Linker): MemoryLayout = when (this) {
    Pointer::class -> ValueLayout.ADDRESS
    `unsigned short`::class -> linker.canonicalLayouts()["short"]!!
    int::class -> linker.canonicalLayouts()["int"]!!
    `unsigned int`::class -> linker.canonicalLayouts()["int"]!!
    `unsigned long`::class -> linker.canonicalLayouts()["long"]!!
    `unsigned __int64`::class -> ValueLayout.JAVA_LONG

    else -> {
        val companion = this.companionObjectInstance
        if (companion is TypeDynamicallySolved) return companion.solve().memoryLayout(linker)

        val superType = this.supertypes.first { !(it.classifier!! as KClass<*>).java.isInterface }.classifier as KClass<*>
        if (superType == Any::class) throw NotImplementedError("Edge: $this")
        superType.memoryLayout(linker)
    }
}

fun Method.type(): MethodType = MethodType.methodType(
    this.returnType,
    this.parameterTypes
)

fun KFunction<*>.handle(): MethodHandle {
    val lookup = MethodHandles.lookup()
    val javaMethod = this.javaMethod!!

    if (javaMethod.modifiers and Modifier.STATIC != 0) TODO("Static function call")

    return lookup.findVirtual(
        javaMethod.declaringClass,
        javaMethod.name, javaMethod.type()
    ).bindTo(javaMethod.declaringClass.kotlin.objectInstance!!)
}

object NativeMethodFilters {
    fun fixPointer(pointer: Pointer<*>?): MemorySegment = pointer?.segment ?: MemorySegment.NULL
    fun fixProviderInt(pointer: ValueProvider): Int = pointer.value as Int

    fun fixLong(long: Long, provider: KClass<*>): Any = provider.primaryConstructor!!.call(long)
    fun fixInt(int: Int, provider: KClass<*>): Any = provider.primaryConstructor!!.call(int)
    fun fixShort(short: Short, provider: KClass<*>): Any = provider.primaryConstructor!!.call(short)

    fun fixSegment(
        segment: MemorySegment, provider: KClass<*>
    ): Any = Pointer(provider, segment)
}

fun fixMethodHandle(
    methodHandle: MethodHandle,
    returnType: KType, parameters: List<KTypeProjection>
): MethodHandle {
    var methodHandle = methodHandle
    parameters.forEachIndexed { index, parameter ->
        val classifier = parameter.type!!.classifier as KClass<*>
        val innerType = methodHandle.type().parameterType(index)
        methodHandle = MethodHandles.filterArguments(
            methodHandle,
            index, (if (Pointer::class.isSuperclassOf(classifier))
                NativeMethodFilters::fixPointer
            else if (ValueProvider::class.isSuperclassOf(classifier))
                when (innerType) {
                    Int::class.java -> NativeMethodFilters::fixProviderInt
                    else -> throw NotImplementedError("$innerType")
                }
            else throw NotImplementedError("$classifier")).handle().asType(
                MethodType.methodType(innerType, (parameter.type!!.classifier as KClass<*>).java)
            )
        )
    }

    val returnClass = returnType.classifier as KClass<*>
    if (returnClass == Unit::class || returnClass == Nothing::class) return methodHandle

    val innerType = methodHandle.type().returnType()
    val fixedType = MethodType.methodType(returnClass.java, innerType)
    methodHandle = MethodHandles.filterReturnValue(
        methodHandle,
        if (Pointer::class.isSuperclassOf(returnClass)) MethodHandles.insertArguments(
                NativeMethodFilters::fixSegment.handle(),
                1, returnType.arguments.first().type!!.classifier as KClass<*>)
            .asType(fixedType)
        else if (ValueProvider::class.isSuperclassOf(returnClass)) MethodHandles.insertArguments(
            when (innerType) {
                Long::class.java -> NativeMethodFilters::fixLong
                Int::class.java -> NativeMethodFilters::fixInt
                else -> throw NotImplementedError("$innerType")
            }.handle(), 1, returnClass)
            .asType(fixedType)
        else throw NotImplementedError("$returnClass")
    )
    return methodHandle
}

inline fun <reified L: LibrarySource> get(
    lookup: SymbolLookup,
    linker: Linker
): L {
    val constructor = L::class.constructors.first()
    val arguments = arrayOfNulls<Any>(constructor.parameters.size)
    constructor.parameters.forEach { parameter ->
        val procedure = lookup.find(parameter.name).getOrNull()

        val classifier = parameter.type.classifier as KClass<*>
        val parameterArguments = parameter.type.arguments

        if (Function::class.isSuperclassOf(classifier)) {
            val parameters = parameterArguments.subList(0, parameterArguments.lastIndex)
            val parametersArray = parameters.map { parameter ->
                (parameter.type!!.classifier as KClass<*>).memoryLayout(linker)
            }.toTypedArray()

            val returnType = parameterArguments.last().type!!
            val returnClass = returnType.classifier as KClass<*>

            var ple = false
            val options = mutableListOf<Linker.Option>()
            for (annotation in parameter.type.annotations) {
                if (annotation is Critical) options.add(Linker.Option.critical(annotation.heapAccess))
                if (annotation is PreserveLastError) {
                    options.add(Linker.Option.captureCallState(LAST_ERROR))
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
                methodHandle = fixMethodHandle(methodHandle, returnType, parameters)
                println(methodHandle)

                arguments[parameter.index] = when (val size = parameterArguments.size) {
                    2 -> object : Function1<Any?, Unit> {
                        override fun invoke(
                            p1: Any?
                        ) {
                            methodHandle.invoke(p1)
                        }
                    }

                    else -> throw NotImplementedError("Function ... $size")
                }
            } else {
                var methodHandle = linker.downcallHandle(
                    procedure,
                    FunctionDescriptor.of(
                        returnClass.memoryLayout(linker),
                        *parametersArray
                    ),
                    *options.toTypedArray()
                )
                if (ple) methodHandle = MethodHandles.insertArguments(methodHandle, 0, StateLayout.local.get())
                methodHandle = fixMethodHandle(methodHandle, returnType, parameters)
                println(methodHandle)

                arguments[parameter.index] = when (val size = parameterArguments.size) {
                    1 -> object : Function0<Any> {
                        override fun invoke(): Any = methodHandle.invoke()
                    }

                    3 -> object : Function2<Any?, Any?, Any> {
                        override fun invoke(
                            p1: Any?, p2: Any?
                        ): Any = methodHandle.invoke(p1, p2)
                    }

                    5 -> object : Function4<Any?, Any?, Any?, Any?, Any> {
                        override fun invoke(
                            p1: Any?, p2: Any?, p3: Any?,
                            p4: Any?
                        ): Any = methodHandle.invoke(p1, p2, p3, p4)
                    }

                    8 -> object : Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any> {
                        override fun invoke(
                            p1: Any?, p2: Any?, p3: Any?,
                            p4: Any?, p5: Any?, p6: Any?,
                            p7: Any?
                        ): Any = methodHandle.invoke(p1, p2, p3, p4, p5, p6, p7)
                    }

                    else -> throw NotImplementedError("Function ... $size")
                }
            }
        } else throw IllegalArgumentException("Unable to translate (unknown): $parameter")
    }
    return constructor.call(*arguments)
}