package org.bread_experts_group.bread_systems_library.ffm.windows.library

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.windows.PreserveLastError
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.windef.LPRECT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser.LPMSG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser.MSG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser.WNDCLASSEXW
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused", "LocalVariableName")
class User32(
    override val linker: Linker,
    val RegisterClassExW: @PreserveLastError ((unnamedParam1: Pointer<WNDCLASSEXW>?) -> ATOM)?,
    val CreateWindowExW: @PreserveLastError ((dwExStyle: DWORD, lpClassName: LPCWSTR?, lpWindowName: LPCWSTR?, dwStyle: DWORD, X: int, Y: int, nWidth: int, nHeight: int, hWndParent: HWND?, hMenu: HMENU?, hInstance: HINSTANCE?, lpParam: LPVOID?) -> HWND)?,
    val DefWindowProcW: ((hWnd: HWND, Msg: UINT, wParam: WPARAM, lParam: LPARAM) -> LRESULT)?,
    val GetMessageW: @PreserveLastError ((lpMsg: LPMSG?, hWnd: HWND?, wMsgFilterMin: UINT, wMsgFilterMax: UINT) -> BOOL)?,
    val TranslateMessage: ((lpMsg: Pointer<MSG>) -> BOOL)?,
    val DispatchMessageW: ((lpMsg: Pointer<MSG>) -> LRESULT)?,
    val GetClientRect: @PreserveLastError ((hWnd: HWND?, lpRect: LPRECT?) -> BOOL)?,
) : LibrarySource