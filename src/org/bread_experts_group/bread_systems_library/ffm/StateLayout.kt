package org.bread_experts_group.bread_systems_library.ffm

import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment

object StateLayout {
    private val state = Linker.Option.captureStateLayout()
    val local: ThreadLocal<MemorySegment> = ThreadLocal.withInitial { Arena.global().allocate(state) }

    const val LAST_ERROR = "GetLastError"

    fun get(name: String): Any = state.varHandle(groupElement(name)).get(local.get(), 0L)
}