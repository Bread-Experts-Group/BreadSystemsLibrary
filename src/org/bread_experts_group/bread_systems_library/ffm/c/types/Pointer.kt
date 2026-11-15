package org.bread_experts_group.bread_systems_library.ffm.c.types

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class Pointer<T : Any>(
    private val linker: Linker,
    private val encoded: KType,
    val segment: MemorySegment
) {
    @Suppress("UNCHECKED_CAST")
    val value: T
        get() = image(linker, encoded, segment) as T

    override fun toString(): String = "$segment -> Pointer<$encoded>${if (encoded.classifier != Void::class) "($value)" else ""}"
}

inline fun <reified T : Structure> Pointer(arena: SegmentAllocator, linker: Linker = Linker.nativeLinker()): Pointer<T> = Pointer(
    linker,
    typeOf<T>(),
    arena.allocate(typeOf<T>().computeLayout(linker))
)

inline fun <reified T : Pointer<*>> indirectPointer(arena: SegmentAllocator, linker: Linker = Linker.nativeLinker()): Pointer<T> = Pointer(
    linker,
    typeOf<T>(),
    arena.allocate(ValueLayout.ADDRESS)
)

inline fun <reified T : IUnknown> comPointer(arena: SegmentAllocator, linker: Linker = Linker.nativeLinker()): Pointer<T> = Pointer(
    linker,
    typeOf<T>(),
    arena.allocate(ValueLayout.ADDRESS)
)

inline fun <reified T : Any> Pointer(segment: MemorySegment, linker: Linker = Linker.nativeLinker()) : Pointer<T> = Pointer(
    linker,
    typeOf<T>(),
    segment
)