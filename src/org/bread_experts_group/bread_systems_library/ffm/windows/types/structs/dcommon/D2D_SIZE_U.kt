package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface D2D_SIZE_U : Structure {
    @Order(0) var width: UINT32
    @Order(1) var height: UINT32
}