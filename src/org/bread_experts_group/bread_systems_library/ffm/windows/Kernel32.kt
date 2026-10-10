package org.bread_experts_group.bread_systems_library.ffm.windows

import org.bread_experts_group.bread_systems_library.ffm.Critical
import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.va_list
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ioringapi.HIORING
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ntioring_x.IORING_OP_CODE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.FILETIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.LPFILETIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.LPSECURITY_ATTRIBUTES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase.LPSYSTEMTIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ntioring_x.IORING_BUFFER_INFO
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FILE_WRITE_FLAGS
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused", "LocalVariableName")
class Kernel32(
    override val linker: Linker,
    val LocalFree: ((hMem: HLOCAL?) -> HLOCAL)?,
    val GetTickCount64: (() -> ULONGLONG)?,
    val GetCurrentProcessId: (() -> DWORD)?,
    val GetCurrentProcess: (() -> HANDLE)?,
    val WaitForSingleObject: ((hHandle: HANDLE?, dwMilliseconds: DWORD) -> DWORD)?,
    val WaitForMultipleObjectsEx: @PreserveLastError ((nCount: DWORD, lpHandles: Pointer<HANDLE>?, bWaitAll: BOOL, dwMilliseconds: DWORD, bAlertable: BOOL) -> DWORD)?,
//    val HeapCreate: @PreserveLastError ((flOptions: DWORD, dwInitialSize: SIZE_T, dwMaximumSize: SIZE_T) -> HANDLE)?,
//    val HeapAlloc: @PreserveLastError ((hHeap: HANDLE?, dwFlags: DWORD, dwBytes: SIZE_T) -> LPVOID)?,
    val VirtualFreeEx: @PreserveLastError ((hProcess: HANDLE?, lpAddress: LPVOID?, dwSize: SIZE_T, dwFreeType: DWORD) -> BOOL)?,
    val CloseHandle: @PreserveLastError ((hObject: HANDLE?) -> BOOL)?,
    val GetConsoleCP: @PreserveLastError (() -> UINT)?,
    val SetEvent: @PreserveLastError ((hEvent: HANDLE?) -> BOOL)?,
    val CreateEventW: @PreserveLastError ((lpEventAttributes: LPSECURITY_ATTRIBUTES?, bManualReset: BOOL, bInitialState: BOOL, lpName: LPCWSTR?) -> HANDLE)?,
    val CreateEventExW: @PreserveLastError ((lpEventAttributes: LPSECURITY_ATTRIBUTES?, lpName: LPCWSTR?, dwFlags: DWORD, dwDesiredAccess: DWORD) -> HANDLE)?,
    val FormatMessageW: @PreserveLastError ((dwFlags: DWORD, lpSource: LPCVOID?, dwMessageId: DWORD, dwLanguageId: DWORD, lpBuffer: LPWSTR?, nSize: DWORD, Arguments: Pointer<va_list>?) -> DWORD)?,
    val CreateFileW: @PreserveLastError ((lpFileName: LPCWSTR?, dwDesiredAccess: DWORD, dwShareMode: DWORD, lpSecurityAttributes: LPSECURITY_ATTRIBUTES?, dwCreationDisposition: DWORD, dwFlagsAndAttributes: DWORD, hTemplateFile: HANDLE?) -> HANDLE)?,
    val GetFileTime: @PreserveLastError ((hFile: HANDLE?, lpCreationTime: LPFILETIME?, lpLastAccessTime: LPFILETIME?, lpLastWriteTime: LPFILETIME?) -> BOOL)?,
    val FileTimeToSystemTime: @PreserveLastError ((lpFileTime: Pointer<FILETIME>?, lpSystemTime: LPSYSTEMTIME?) -> BOOL)?,
    val ExitProcess: @Critical(false) ((uExitCode: UINT) -> Nothing)?,
    val CreateIoRing: ((ioringVersion: IORING_VERSION, flags: IORING_CREATE_FLAGS, submissionQueueSize: UINT32, completionQueueSize: UINT32, h: Pointer<HIORING>?) -> HRESULT)?,
    val CloseIoRing: ((ioRing: HIORING?) -> HRESULT)?,
    val QueryIoRingCapabilities: ((capabilities: Pointer<IORING_CAPABILITIES>?) -> HRESULT)?,
    val IsIoRingOpSupported: ((ioRing: HIORING?, op: IORING_OP_CODE) -> BOOL)?,
    val SubmitIoRing: ((ioRing: HIORING?, waitOperations: UINT32, milliseconds: UINT32, submittedEntries: Pointer<UINT32>?) -> HRESULT)?,
    val PopIoRingCompletion: ((ioRing: HIORING?, cqe: Pointer<IORING_CQE>?) -> HRESULT)?,
    val BuildIoRingRegisterBuffers: ((ioRing: HIORING?, count: UINT32, buffers: Pointer<IORING_BUFFER_INFO>?, userData: UINT_PTR) -> HRESULT)?,
    val BuildIoRingRegisterFileHandles: ((ioRing: HIORING?, count: UINT32, handles: Pointer<HANDLE>?, userData: UINT_PTR) -> HRESULT)?,
    val BuildIoRingReadFile: ((ioRing: HIORING?, fileRef: IORING_HANDLE_REF?, dataRef: IORING_BUFFER_REF?, numberOfBytesToRead: UINT32, fileOffset: UINT64, userData: UINT_PTR, sqeFlags: IORING_SQE_FLAGS) -> HRESULT)?,
    val BuildIoRingWriteFile: ((ioRing: HIORING?, fileRef: IORING_HANDLE_REF?, bufferRef: IORING_BUFFER_REF?, numberOfBytesToWrite: UINT32, fileOffset: UINT64, writeFlags: FILE_WRITE_FLAGS, userData: UINT_PTR, sqeFlags: IORING_SQE_FLAGS) -> HRESULT)?,
    val SetIoRingCompletionEvent: ((ioRing: HIORING?, hEvent: HANDLE?) -> HRESULT)?
) : LibrarySource