package org.bread_experts_group.bread_systems_library.ffm.util.layout

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.memoryLayout
import org.bread_experts_group.bread_systems_library.ffm.methodHandleNativeToJVM
import org.bread_experts_group.bread_systems_library.ffm.util.ArrayElement
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.interpretCOM
import org.bread_experts_group.bread_systems_library.generic.ArraySize
import org.bread_experts_group.bread_systems_library.generic.Order
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaSetter

fun MemoryLayout.writeDirect(into: MemorySegment, offset: Long, using: Any) {
    when (this) {
        is StructLayout -> {
            val using = if (using is Pointer<*>) using.segment else using as MemorySegment

            var usingOffset = 0L
            this.memberLayouts().forEach { layout ->
                layout.writeIndirect(into, offset, using.asSlice(usingOffset))
                usingOffset += layout.byteSize()
            }
        }

        is AddressLayout -> {
            val using = if (using is Pointer<*>) using.segment else using as MemorySegment
            into[this, offset] = using
        }

        else -> throw NotImplementedError("${this::class}")
    }
}

fun MemoryLayout.writeIndirect(into: MemorySegment, offset: Long, from: MemorySegment) {
    when (this) {
        is ValueLayout.OfInt -> into[this, offset] = from.get(this, 0)
        is AddressLayout -> into[this, offset] = from.get(this, 0)
        is PaddingLayout -> {}

        else -> throw NotImplementedError("${this::class}")
    }
}

fun KType.computeLayout(
    linker: Linker,
    padEnd: Boolean = true,
    unaligned: Boolean = false
): MemoryLayout {
    val clazz = this.classifier!! as KClass<*>
    if (clazz == ArrayElement::class) return MemoryLayout.sequenceLayout(
        this.findAnnotation<ArraySize>()!!.size,
        this.arguments[0].type!!.memoryLayout(linker, padEnd, unaligned)
    )

    val layouts = mutableListOf<MemoryLayout>()
    clazz.memberProperties.sortedBy { it.findAnnotation<Order>()!!.order }.forEach {
        layouts.add(
            resolveType(it.returnType, this)
                .memoryLayout(linker, padEnd, unaligned)
                .withName(it.name)
        )
    }

    var localOffset = 0L
    var largestAlignment = 0L
    val iterator = layouts.listIterator()
    for (layout in iterator) {
        val align = layout.byteAlignment()
        if (align > largestAlignment) largestAlignment = align
        val remainder = localOffset % align
        if (remainder != 0L) {
            val realign = align - remainder
            iterator.set(MemoryLayout.paddingLayout(realign))
            iterator.add(layout)
            localOffset += realign
        }
        localOffset += layout.byteSize()
    }
    if (padEnd) {
        val remainder = localOffset % largestAlignment
        if (remainder != 0L) layouts.add(
            MemoryLayout.paddingLayout(largestAlignment - remainder)
        )
    }

    val layout = if (Union::class.isSuperclassOf(clazz)) MemoryLayout.unionLayout(
        *layouts.toTypedArray()
    ) else MemoryLayout.structLayout(
        *layouts.toTypedArray()
    )
    return layout.withName(clazz.simpleName).let {
        if (unaligned) it.withByteAlignment(1) else it
    }
}

private val imageCache = mutableMapOf<LayoutConfiguration, (MemorySegment) -> Any>()

private fun imageDry(linker: Linker, encoded: KType, unaligned: Boolean = false): (MemorySegment) -> Any {
    val encodedClass = encoded.classifier as KClass<*>
    if (encodedClass == Nothing::class) throw UnsupportedOperationException("Opaque pointer")
    if (Structure::class.isSuperclassOf(encodedClass)) {
        val layout = encoded.computeLayout(linker, unaligned = unaligned)

        val methods = mutableMapOf<Method, ImageMethodKind>()
        encodedClass.memberProperties.forEach {
            val resolvedType = resolveType(it.returnType, encoded)
            if (Structure::class.isSuperclassOf(resolvedType.classifier!! as KClass<*>)) {
                val handle = layout.sliceHandle(groupElement(it.name))
                if (it is KMutableProperty<*>) methods[it.javaSetter!!] = ImageMethodKind.Setter { structureSegment, a: Any ->
                    a as Structure
                    throw NotImplementedError("Structure copy ${a.segment()} -> $structureSegment")
                }
                methods[it.javaGetter!!] = ImageMethodKind.Struct { structureSegment ->
                    image(
                        linker,
                        resolvedType,
                        handle(structureSegment, 0L) as MemorySegment
                    )
                }
            } else {
                val handle = layout.varHandle(groupElement(it.name))
                val clazz = resolvedType.classifier as KClass<*>
                if (it is KMutableProperty<*>) {
                    val handler = when (val layout = resolvedType.memoryLayout(linker)) {
                        ValueLayout.JAVA_BYTE -> if (clazz == Byte::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as ValueProvider).value as Byte)
                        }

                        ValueLayout.JAVA_SHORT -> if (clazz == Short::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as ValueProvider).value as Short)
                        }

                        ValueLayout.JAVA_INT -> if (clazz == Int::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as ValueProvider).value as Int)
                        }

                        ValueLayout.JAVA_LONG -> if (clazz == Long::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as ValueProvider).value as Long)
                        }

                        ValueLayout.JAVA_FLOAT -> if (clazz == Float::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as ValueProvider).value as Float)
                        }

                        ValueLayout.ADDRESS -> if (clazz == Pointer::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as Pointer<*>).segment)
                        } else { structureSegment: MemorySegment, function: Any ->
                            function as KFunction<*>
                            val upcall = linker.upcallStub(
                                methodHandleNativeToJVM(function, MethodHandles.lookup(), linker),
                                FunctionDescriptor.of(
                                    function.returnType.memoryLayout(linker, false),
                                    *function.parameters.map { parameter ->
                                        parameter.type.memoryLayout(linker, false)
                                    }.toTypedArray()
                                ),
                                Arena.global()
                            )
                            handle.set(structureSegment, 0L, upcall)
                        }

                        else -> throw NotImplementedError("$layout")
                    }

                    methods[it.javaSetter!!] = ImageMethodKind.Setter(handler)
                }

                methods[it.javaGetter!!] = when (clazz) {
                    Byte::class, Short::class, Int::class, Long::class -> ImageMethodKind.Getter { structureSegment ->
                        handle.get(structureSegment, 0L)
                    }

                    else -> ImageMethodKind.Getter { structureSegment ->
                        clazz.primaryConstructor!!.call(handle.get(structureSegment, 0L))
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return {
            Proxy.newProxyInstance(
                encodedClass.java.classLoader,
                arrayOf(encodedClass.java)
            ) { proxy, method, args ->
                when (method) {
                    Object::toString.javaMethod!! -> return@newProxyInstance if (proxy is ArrayElement<*>) {
                        val size = encoded.findAnnotation<ArraySize>()!!.size
                        var constructed = "$it -> ($size)["
                        for (i in 0..<size) constructed += (if (i > 0) ", ${proxy[i]}" else proxy[i])
                        "$constructed]"
                    } else "$it -> ${encodedClass.simpleName}(${encodedClass.memberProperties
                        .sortedBy { prop -> prop.findAnnotation<Order>()!!.order }
                        .joinToString { prop ->
                            "${prop.name}: ${(prop as KProperty1<Any, Any>).get(proxy)}"
                        }})"
                    Object::hashCode.javaMethod!! -> return@newProxyInstance encodedClass.memberProperties.map { prop ->
                        (prop as KProperty1<Any, Any>).get(proxy)
                    }.toTypedArray().contentHashCode()
                    Object::equals.javaMethod!! -> {
                        val other = args[0]
                        if (!encodedClass.isInstance(other)) return@newProxyInstance false
                        encodedClass.memberProperties.forEach { prop ->
                            val prop = prop as KProperty1<Any, Any>
                            if (prop.get(proxy) != prop.get(other)) return@newProxyInstance false
                        }
                        return@newProxyInstance true
                    }

                    Structure::segment.javaMethod!! -> return@newProxyInstance it
                    Structure::setUpcall.javaMethod!! -> {
                        val property = args[0] as KProperty<*>
                        val upcallHandle = args[1] as MethodHandle
//                        if (property !in encodedClass.memberProperties) throw IllegalArgumentException(
//                            "$encoded does not contain $property"
//                        )
                        val parameters = property.returnType.arguments.dropLast(1).map { p -> p.type!! }
                        val returnType = property.returnType.arguments.last().type!!

                        val upcall = linker.upcallStub(
                            methodHandleNativeToJVM(
                                upcallHandle,
                                parameters,
                                returnType,
                                MethodHandles.lookup(), linker
                            ),
                            FunctionDescriptor.of(
                                returnType.memoryLayout(linker, false),
                                *parameters.map { p -> p.memoryLayout(linker, false) }.toTypedArray()
                            ),
                            Arena.global()
                        )
                        layout.varHandle(groupElement(property.name)).set(it, 0L, upcall)
                        return@newProxyInstance null
                    }

                    ArrayElement<*>::get.javaMethod!! -> {
                        val elementType = encoded.arguments[0].type!!
                        val layout = elementType.memoryLayout(linker, unaligned = unaligned)
                        return@newProxyInstance image(
                            linker,
                            elementType,
                            it.asSlice(layout.byteSize() * args[0] as Long, layout)
                        )
                    }

                    ArrayElement<*>::set.javaMethod!! -> {
                        val elementType = encoded.arguments[0].type!!
                        if (
                            (elementType.classifier!! as KClass<*>).isSubclassOf(ValueProvider::class)
                        ) {
                            val value = (args[1] as ValueProvider).value
                            when (val layout = elementType.memoryLayout(linker, unaligned = unaligned)) {
                                ValueLayout.JAVA_BYTE -> it.setAtIndex(
                                    ValueLayout.JAVA_BYTE, args[0] as Long, value as Byte
                                )

                                else -> throw NotImplementedError("$layout")
                            }
                        } else throw NotImplementedError("$elementType")
                        return@newProxyInstance null
                    }
                }

                when (val method = methods[method]!!) {
                    is ImageMethodKind.Getter -> {
                        return@newProxyInstance method.handler(it)
                    }

                    is ImageMethodKind.Setter -> {
                        method.handler(it, args[0])
                        return@newProxyInstance null
                    }

                    is ImageMethodKind.Struct -> return@newProxyInstance method.image(it)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    if (encodedClass == Pointer::class) return { structureSegment ->
        Pointer<Any>(linker, encoded.arguments[0].type!!, structureSegment.get(ValueLayout.ADDRESS, 0))
    }

    @Suppress("UNCHECKED_CAST")
    if (encodedClass.isSubclassOf(ValueProvider::class)) {
        val layout = encoded.memoryLayout(linker, unaligned = unaligned)
        val constructor = encodedClass.primaryConstructor!!
        return when (layout) {
            ValueLayout.JAVA_BYTE -> { structureSegment -> constructor.call(structureSegment.get(ValueLayout.JAVA_BYTE, 0)) }

            else -> throw NotImplementedError("$layout")
        }
    }

    if (encodedClass.isSubclassOf(IUnknown::class)) return { segment ->
        interpretCOM(linker, encoded, segment[ValueLayout.ADDRESS, 0].reinterpret(ValueLayout.ADDRESS.byteSize()))
    }

    throw NotImplementedError("$encoded")
}

fun image(linker: Linker, encoded: KType, segment: MemorySegment, unaligned: Boolean = false): Any = imageCache.getOrPut(
    LayoutConfiguration(linker, unaligned, @OptIn(ExperimentalStdlibApi::class) encoded.javaType)
) {
    imageDry(linker, encoded, unaligned)
}(segment)

inline fun <reified T : Any> image(linker: Linker, segment: MemorySegment): T = image(linker, typeOf<T>(), segment) as T

fun Array(
    type: KType, arrayType: KType,
    arena: SegmentAllocator, count: Int,
    linker: Linker = Linker.nativeLinker(),
    initializer: (Int, Any) -> Any
): Pair<Array<Any>, MemorySegment> {
    val array = java.lang.reflect.Array.newInstance((arrayType.classifier!! as KClass<*>).java, count)
    val layout = type.computeLayout(linker, true)

    val segment = arena.allocate(layout, count.toLong())
    for (i in 0..<count) {
        java.lang.reflect.Array.set(
            array, i,
            initializer(i, image(linker, type, segment.asSlice(i * layout.byteSize())))
        )
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<Any> to segment
}

fun NativeArray(
    type: KType,
    arena: SegmentAllocator, count: Int,
    linker: Linker = Linker.nativeLinker(),
    operation: (Int, Any) -> Unit
): MemorySegment {
    val layout = type.computeLayout(linker, true)
    val segment = arena.allocate(layout, count.toLong())
    for (i in 0..<count) {
        operation(i, image(linker, type, segment.asSlice(i * layout.byteSize())))
    }
    @Suppress("UNCHECKED_CAST")
    return segment
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any, reified R : Any> Array(
    arena: SegmentAllocator, count: Int,
    linker: Linker = Linker.nativeLinker(),
    noinline initializer: (Int, T) -> R
) = Array(typeOf<T>(), typeOf<R>(), arena, count, linker, initializer as (Int, Any) -> Any) as Pair<Array<R>, MemorySegment>

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> NativeArray(
    arena: SegmentAllocator, count: Int,
    linker: Linker = Linker.nativeLinker(),
    noinline initializer: (Int, T) -> Unit
) = NativeArray(typeOf<T>(), arena, count, linker, initializer as (Int, Any) -> Unit)

fun resolveType(
    innerType: KType,
    classType: KType
): KType {
    val classifier = innerType.classifier
    if (classifier is KClass<*>) return innerType
    if (classifier is KTypeParameter) return classType.arguments[(classType.classifier!! as KClass<*>).typeParameters.indexOfFirst { kT -> kT == classifier }].type!!
    throw NotImplementedError("$innerType in $classType")
}