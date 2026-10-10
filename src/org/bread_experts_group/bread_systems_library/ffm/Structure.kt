package org.bread_experts_group.bread_systems_library.ffm

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

interface Structure {
    fun segment(): MemorySegment
}

inline fun <reified T : Structure> Structure(
    arena: SegmentAllocator,
    linker: Linker = Linker.nativeLinker()
) = Pointer<T>(arena, linker).value