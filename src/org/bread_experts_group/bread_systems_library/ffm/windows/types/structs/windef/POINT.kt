package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.windef

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.LONG
import org.bread_experts_group.bread_systems_library.generic.Order

interface POINT : Structure {
    @Order(0) var x: LONG
    @Order(1) var y: LONG
}