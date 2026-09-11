@file:Suppress("ClassName")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.BOOL
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.LPVOID
import org.bread_experts_group.bread_systems_library.generic.Order

interface _SECURITY_ATTRIBUTES : Structure {
    @Order(0) val nLength: DWORD
    @Order(1) val lpSecurityDescriptor: LPVOID
    @Order(2) val bInheritHandle: BOOL
}

typealias LPSECURITY_ATTRIBUTES = Pointer<_SECURITY_ATTRIBUTES>