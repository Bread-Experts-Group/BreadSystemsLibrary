package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.PVOID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ULONG_PTR
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface _OVERLAPPED : Structure {
    @Order(0) var Internal: ULONG_PTR
    @Order(1) var InternalHigh: ULONG_PTR
    @Order(2) var DUMMYUNIONNAME: _DUMMYUNIONNAME
    @Order(3) var hEvent: HANDLE

    interface _DUMMYUNIONNAME : Union {
        @Order(0) var DUMMYSTRUCTNAME: _DUMMYSTRUCTNAME
        @Order(1) var Pointer: PVOID

        interface _DUMMYSTRUCTNAME : Structure {
            @Order(0) var Offset: DWORD
            @Order(1) var OffsetHigh: DWORD
        }
    }
}

typealias OVERLAPPED = _OVERLAPPED
typealias LPOVERLAPPED = Pointer<_OVERLAPPED >