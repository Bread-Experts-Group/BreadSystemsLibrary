package org.bread_experts_group.bread_systems_library.ffm.util.layout

import org.bread_experts_group.bread_systems_library.ffm.NativeMethodFilters
import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.memoryLayout
import org.bread_experts_group.bread_systems_library.generic.Order
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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
    alignment: Long? = null
): MemoryLayout {
    val clazz = this.classifier!! as KClass<*>
    val layouts = mutableListOf<MemoryLayout>()
    clazz.memberProperties.sortedBy { it.findAnnotation<Order>()!!.order }.forEach {
        layouts.add(
            resolveType(it.returnType, this)
                .memoryLayout(linker, padEnd, alignment)
                .withName(it.name)
                .let { layout -> if (alignment != null) layout.withByteAlignment(alignment) else layout }
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
        if (alignment != null) it.withByteAlignment(alignment) else it
    }
}

private val imageCache = mutableMapOf<LayoutConfiguration, (MemorySegment) -> Any>()

private fun imageDry(encoded: KType, alignment: Long? = null): (MemorySegment) -> Any {
    val encodedClass = encoded.classifier as KClass<*>
    if (encodedClass == Nothing::class) throw UnsupportedOperationException("Opaque pointer")
    if (Structure::class.isSuperclassOf(encodedClass)) {
        val layout = encoded.computeLayout(Linker.nativeLinker(), alignment = alignment)

        val methods = mutableMapOf<Method, ImageMethodKind>()
        encodedClass.memberProperties.forEach {
            val resolvedType = resolveType(it.returnType, encoded)
            if (Structure::class.isSuperclassOf(resolvedType.classifier!! as KClass<*>)) {
                val handle = layout.sliceHandle(groupElement(it.name))
                if (it is KMutableProperty<*>) TODO("Alpha")
                methods[it.javaGetter!!] = ImageMethodKind.Struct { structureSegment ->
                    image(
                        resolvedType,
                        handle(structureSegment, 0L) as MemorySegment
                    )
                }
            } else {
                val handle = layout.varHandle(groupElement(it.name))
                val clazz = resolvedType.classifier as KClass<*>
                if (it is KMutableProperty<*>) {
                    val handler = when (val layout = resolvedType.memoryLayout(Linker.nativeLinker())) {
                        ValueLayout.JAVA_BYTE -> if (clazz == Byte::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, NativeMethodFilters.fixProviderByte(a as ValueProvider))
                        }

                        ValueLayout.JAVA_SHORT -> if (clazz == Short::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, NativeMethodFilters.fixProviderShort(a as ValueProvider))
                        }

                        ValueLayout.JAVA_INT -> if (clazz == Int::class) { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, a)
                        } else { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, NativeMethodFilters.fixProviderInt(a as ValueProvider))
                        }

                        ValueLayout.JAVA_LONG -> { structureSegment: MemorySegment, a: Any ->
                            TODO("!!")
                        }

                        ValueLayout.ADDRESS -> { structureSegment: MemorySegment, a: Any ->
                            handle.set(structureSegment, 0L, (a as Pointer<*>).segment)
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
                if (method == Object::toString.javaMethod!!) {
                    return@newProxyInstance "$it -> ${encodedClass.simpleName}(${encodedClass.memberProperties
                        .sortedBy { prop -> prop.findAnnotation<Order>()!!.order }
                        .joinToString { prop ->
                        "${prop.name}: ${(prop as KProperty1<Any, Any>).get(proxy)}"
                    }})"
                }

                if (method == Structure::segment.javaMethod!!) {
                    return@newProxyInstance it
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
        Pointer<Any>(structureSegment.get(ValueLayout.ADDRESS, 0))
    }

    throw NotImplementedError("$encoded")
}

fun image(encoded: KType, segment: MemorySegment, alignment: Long? = null): Any = imageCache.getOrPut(
    LayoutConfiguration(alignment, @OptIn(ExperimentalStdlibApi::class) encoded.javaType)
) {
    imageDry(encoded, alignment)
}(segment)

inline fun <reified T : Any> image(segment: MemorySegment): T = image(typeOf<T>(), segment) as T

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
            initializer(i, image(type, segment.asSlice(i * layout.byteSize())))
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
        operation(i, image(type, segment.asSlice(i * layout.byteSize())))
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