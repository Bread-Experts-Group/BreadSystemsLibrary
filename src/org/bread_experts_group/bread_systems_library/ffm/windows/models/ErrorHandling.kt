package org.bread_experts_group.bread_systems_library.ffm.windows.models

import org.bread_experts_group.bread_systems_library.ffm.StateLayout
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.LAST_ERROR
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.models.exception.hResult
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FORMAT_MESSAGE_ALLOCATE_BUFFER
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FORMAT_MESSAGE_FROM_SYSTEM
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FORMAT_MESSAGE_IGNORE_INSERTS
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.*
import kotlin.reflect.typeOf

fun handleLastError(kernel32: Kernel32) = handleResult(kernel32, DWORD(StateLayout.get(LAST_ERROR)))

private val localBuffer = WeakHashMap<Kernel32, ThreadLocal<Pointer<WCHAR>>>()

fun getResult(kernel32: Kernel32, n: DWORD, cause: Throwable? = null): Throwable? {
    n.value as Int
    val buffer = synchronized(localBuffer) {
        localBuffer.getOrPut(kernel32) {
            ThreadLocal.withInitial {
                Pointer<WCHAR>(Arena.ofConfined().allocate(ValueLayout.ADDRESS))
            }
        }.get()
    }
    val count = kernel32.FormatMessageW!!(
        DWORD(
            FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS or FORMAT_MESSAGE_ALLOCATE_BUFFER
        ), null, n,
        DWORD(0), buffer, DWORD(0),
        null
    ).value as Int
    if (count == 0) throw IllegalStateException("Failed to retrieve system message")
    val segment = buffer.segment.get(ValueLayout.ADDRESS, 0).reinterpret((count + 1) * 2L)
    val message = segment.getString(0, Charsets.UTF_16LE)
    val new = kernel32.LocalFree!!(
        HANDLE(typeOf<Void>(), segment)
    )
    if (new.segment != MemorySegment.NULL) throw IllegalStateException("Failed to free message")
    return hResult(n.value, message.trim()).initCause(cause)
}

fun handleResult(kernel32: Kernel32, n: HRESULT, cause: Throwable? = null) = handleResult(kernel32, DWORD(n.value), cause)

fun handleResult(kernel32: Kernel32, n: DWORD, cause: Throwable? = null) = getResult(kernel32, n, cause)?.let {
    throw it
}