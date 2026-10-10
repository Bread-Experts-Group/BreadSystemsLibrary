package org.bread_experts_group.bread_systems_library.ffm.windows.models

import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import java.lang.ref.Cleaner
import kotlin.reflect.typeOf

class CleanableHandle(
    kernel32: Kernel32,
    handle: HANDLE
) : HANDLE(typeOf<Void>(), handle.segment), AutoCloseable {
    private companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    private class State(
        val kernel32: Kernel32,
        val handle: HANDLE
    ) : Runnable {
        override fun run() {
            if (kernel32.CloseHandle!!(handle).value as Int == 0) handleLastError(kernel32)
        }
    }

    private val cleanable = cleaner.register(this, State(kernel32, handle))
    override fun close() {
        cleanable.clean()
    }
}