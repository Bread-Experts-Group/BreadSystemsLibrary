package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winnt

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface MEM_EXTENDED_PARAMETER : Structure {
    @Order(0) var Type: DWORD64 // TODO: BITFIELD TYPES
    @Order(1) var Union: DUMMYUNIONNAME

    interface DUMMYUNIONNAME : Union {
        @Order(0) var ULong64: DWORD64
        @Order(1) var Pointer: PVOID
        @Order(2) var Size: SIZE_T
        @Order(3) var Handle: HANDLE
        @Order(4) var ULong: DWORD
    }
}

typealias PMEM_EXTENDED_PARAMETER = Pointer<MEM_EXTENDED_PARAMETER>