package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HWND
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_SIZE_U
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D1_HWND_RENDER_TARGET_PROPERTIES : Structure {
    @Order(0) var hwnd: HWND
    @Order(1) var pixelSize: D2D1_SIZE_U
    @Order(2) var presentOptions: D2D1_PRESENT_OPTIONS
}