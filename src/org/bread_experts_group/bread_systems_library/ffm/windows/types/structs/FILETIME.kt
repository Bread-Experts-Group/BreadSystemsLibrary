@file:Suppress("unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface _FILETIME : Structure {
    @Order(0) var dwLowDateTime: DWORD
    @Order(1) var dwHighDateTime: DWORD
}

typealias FILETIME = _FILETIME
typealias LPFILETIME = Pointer<_FILETIME>
typealias PFILETIME = Pointer<_FILETIME>