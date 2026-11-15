package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dxgiformat.DXGI_FORMAT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D1_PIXEL_FORMAT : Structure {
    @Order(0) val format: DXGI_FORMAT
    @Order(1) val alphaMode: D2D1_ALPHA_MODE
}