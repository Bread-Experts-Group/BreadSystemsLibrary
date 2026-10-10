package org.bread_experts_group.bread_systems_library.ffm.c.types

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class Pointer<T : Any>(private val encoded: KType, val segment: MemorySegment) {
    val value: T by lazy {
        @Suppress("UNCHECKED_CAST")
        image(encoded, segment) as T
    }
}

inline fun <reified T : Structure> Pointer(arena: SegmentAllocator, linker: Linker = Linker.nativeLinker()): Pointer<T> {
    return Pointer(typeOf<T>(), arena.allocate(typeOf<T>().computeLayout(linker)))
}

inline fun <reified T : Pointer<*>> Pointer(arena: SegmentAllocator) : Pointer<T> = Pointer(
    typeOf<T>(),
    arena.allocate(ValueLayout.ADDRESS)
)

inline fun <reified T : Any> Pointer(segment: MemorySegment) : Pointer<T> = Pointer(
    typeOf<T>(),
    segment
)