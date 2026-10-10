package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_HANDLE_REF : Structure {
    @Order(0) var Kind: IORING_REF_KIND
    @Order(1) val Handle: HandleUnion

    interface HandleUnion : Union {
        @Order(0) var Handle: HANDLE
        @Order(1) var Index: UINT32
    }
}