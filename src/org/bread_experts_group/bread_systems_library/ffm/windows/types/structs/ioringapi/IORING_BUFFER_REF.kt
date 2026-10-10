package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.c.types.void
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ntioring_x.IORING_REGISTERED_BUFFER
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_BUFFER_REF : Structure {
    @Order(0) var Kind: IORING_REF_KIND
    @Order(1) val Buffer: BufferUnion

    interface BufferUnion : Union {
        @Order(0) var Address: void
        @Order(1) val IndexAndOffset: IORING_REGISTERED_BUFFER
    }
}