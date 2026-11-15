@file:Suppress("SpellCheckingInspection", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.windef.POINT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface tagMSG : Structure {
    @Order(0) var hwnd: HWND
    @Order(1) var message: UINT
    @Order(2) var wParam: WPARAM
    @Order(3) var lParam: LPARAM
    @Order(4) var time: DWORD
    @Order(5) var pt: POINT
}

typealias MSG = tagMSG
typealias PMSG = Pointer<tagMSG>
typealias NPMSG = Pointer<tagMSG>
typealias LPMSG = Pointer<tagMSG>