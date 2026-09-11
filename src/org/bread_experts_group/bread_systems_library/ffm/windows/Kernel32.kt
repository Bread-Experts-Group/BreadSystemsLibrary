package org.bread_experts_group.bread_systems_library.ffm.windows

import org.bread_experts_group.bread_systems_library.ffm.Critical
import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.FILETIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.LPFILETIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.LPSECURITY_ATTRIBUTES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase.LPSYSTEMTIME

@Suppress("PropertyName", "unused")
class Kernel32(
    val GetTickCount64: (() -> ULONGLONG)?,
    val GetCurrentProcessId: (() -> DWORD)?,
    val GetConsoleCP: @PreserveLastError (() -> UINT)?,
    val CreateFileW: @PreserveLastError ((lpFileName: LPCWSTR?, dwDesiredAccess: DWORD, dwShareMode: DWORD, lpSecurityAttributes: LPSECURITY_ATTRIBUTES?, dwCreationDisposition: DWORD, dwFlagsAndAttributes: DWORD, hTemplateFile: HANDLE?) -> HANDLE)?,
    val GetFileTime: @PreserveLastError ((hFile: HANDLE?, lpCreationTime: LPFILETIME?, lpLastAccessTime: LPFILETIME?, lpLastWriteTime: LPFILETIME?) -> BOOL)?,
    val FileTimeToSystemTime: @PreserveLastError ((lpFileTime: Pointer<FILETIME>?, lpSystemTime: LPSYSTEMTIME?) -> BOOL)?,
    val ExitProcess: @Critical(false) ((uExitCode: UINT) -> Nothing)?
) : LibrarySource