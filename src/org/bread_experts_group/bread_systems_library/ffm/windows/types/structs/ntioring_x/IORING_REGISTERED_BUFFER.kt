package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ntioring_x

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_REGISTERED_BUFFER : Structure {
    @Order(0) var BufferIndex: UINT32
    @Order(1) var Offset: UINT32
}