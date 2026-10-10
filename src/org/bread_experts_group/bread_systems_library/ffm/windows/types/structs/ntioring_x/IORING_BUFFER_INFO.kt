package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ntioring_x

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.void
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_BUFFER_INFO : Structure {
    @Order(0) var Address: void
    @Order(1) var Length: UINT32
}