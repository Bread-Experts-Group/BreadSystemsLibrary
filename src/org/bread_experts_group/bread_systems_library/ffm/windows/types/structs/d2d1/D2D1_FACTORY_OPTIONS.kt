package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D1_FACTORY_OPTIONS : Structure {
    @Order(0) val debugLevel: D2D1_DEBUG_LEVEL
}