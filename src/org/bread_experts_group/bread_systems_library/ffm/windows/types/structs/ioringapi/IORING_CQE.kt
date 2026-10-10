package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT_PTR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ULONG_PTR
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_CQE : Structure {
    @Order(0) var UserData: UINT_PTR
    @Order(1) var ResultCode: HRESULT
    @Order(2) var Information: ULONG_PTR
}