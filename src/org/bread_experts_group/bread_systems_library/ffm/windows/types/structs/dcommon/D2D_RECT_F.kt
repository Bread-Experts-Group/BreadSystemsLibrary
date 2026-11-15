package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D_RECT_F : Structure {
    @Order(0) var left: FLOAT
    @Order(1) var top: FLOAT
    @Order(2) var right: FLOAT
    @Order(3) var bottom: FLOAT
}