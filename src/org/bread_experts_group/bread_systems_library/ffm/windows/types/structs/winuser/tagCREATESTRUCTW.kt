@file:Suppress("SpellCheckingInspection", "ClassName", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.generic.Order

interface tagCREATESTRUCTW : Structure {
    @Order(0) var lpCreateParams: LPVOID
    @Order(1) var hInstance: HINSTANCE
    @Order(2) var hMenu: HMENU
    @Order(3) var hwndParent: HWND
    @Order(4) var cy: int
    @Order(5) var cx: int
    @Order(6) var y: int
    @Order(7) var x: int
    @Order(8) var style: LONG
    @Order(9) var lpszName: LPCWSTR
    @Order(10) var lpszClass: LPCWSTR
    @Order(11) var dwExStyle: DWORD
}

typealias CREATESTRUCTW = tagCREATESTRUCTW
typealias LPCREATESTRUCTW = Pointer<tagCREATESTRUCTW>