package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon.D2D1_PIXEL_FORMAT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D1_RENDER_TARGET_PROPERTIES : Structure {
    @Order(0) val type: D2D1_RENDER_TARGET_TYPE
    @Order(1) val pixelFormat: D2D1_PIXEL_FORMAT
    @Order(2) val dpiX: FLOAT
    @Order(3) val dpiY: FLOAT
    @Order(4) val usage: D2D1_RENDER_TARGET_USAGE
    @Order(5) val minLevel: D2D1_FEATURE_LEVEL
}