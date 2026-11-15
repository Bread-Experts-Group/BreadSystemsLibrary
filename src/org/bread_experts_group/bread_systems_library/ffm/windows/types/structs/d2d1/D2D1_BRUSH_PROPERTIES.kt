@file:Suppress("unused", "ClassName")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_MATRIX_3X2_F
import org.bread_experts_group.bread_systems_library.generic.Order

interface D2D1_BRUSH_PROPERTIES : Structure {
    @Order(0) var opacity: FLOAT
    @Order(1) var transform: D2D1_MATRIX_3X2_F
}