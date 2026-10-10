package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winnt

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.PVOID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.SIZE_T
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface _MEM_ADDRESS_REQUIREMENTS : Structure {
    @Order(0) var LowestStartingAddress: PVOID
    @Order(1) var HighestEndingAddress: PVOID
    @Order(2) var Alignment: SIZE_T
}

typealias MEM_ADDRESS_REQUIREMENTS = _MEM_ADDRESS_REQUIREMENTS
typealias PMEM_ADDRESS_REQUIREMENTS = Pointer<_MEM_ADDRESS_REQUIREMENTS>