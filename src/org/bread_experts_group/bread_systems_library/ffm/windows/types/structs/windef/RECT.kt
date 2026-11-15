@file:Suppress("ClassName", "SpellCheckingInspection", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.windef

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.LONG
import org.bread_experts_group.bread_systems_library.generic.Order

interface tagRECT : Structure {
    @Order(0) var left: LONG
    @Order(1) var top: LONG
    @Order(2) var right: LONG
    @Order(3) var bottom: LONG
}

typealias RECT = tagRECT
typealias PRECT = Pointer<tagRECT>
typealias NPRECT = Pointer<tagRECT>
typealias LPRECT = Pointer<tagRECT>