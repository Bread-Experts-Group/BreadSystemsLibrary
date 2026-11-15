@file:Suppress("unused", "ClassName")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.generic.Order

interface D2D_POINT_2F : Structure {
    @Order(0) var x: FLOAT
    @Order(1) var y: FLOAT
}