@file:Suppress("SpellCheckingInspection", "unused", "ClassName")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winuser.WNDPROC
import org.bread_experts_group.bread_systems_library.generic.Order

interface tagWNDCLASSEXW : Structure {
    @Order(0) var cbSize: UINT
    @Order(1) var style: UINT
    @Order(2) var lpfnWndProc: WNDPROC
    @Order(3) var cbClsExtra: int
    @Order(4) var cbWndExtra: int
    @Order(5) var hInstance: HINSTANCE
    @Order(6) var hIcon: HICON
    @Order(7) var hCursor: HCURSOR
    @Order(8) var hbrBackground: HBRUSH
    @Order(9) var lpszMenuName: LPCWSTR
    @Order(10) var lpszClassName: LPCWSTR
    @Order(11) var hIconSm: HICON
}

typealias WNDCLASSEXW = tagWNDCLASSEXW
typealias PWNDCLASSEXW = Pointer<tagWNDCLASSEXW>
typealias NPWNDCLASSEXW = Pointer<tagWNDCLASSEXW>
typealias LPWNDCLASSEXW = Pointer<tagWNDCLASSEXW>