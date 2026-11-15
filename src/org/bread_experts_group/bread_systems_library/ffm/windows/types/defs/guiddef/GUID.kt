package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.unsigned_char
import org.bread_experts_group.bread_systems_library.ffm.c.types.unsigned_long
import org.bread_experts_group.bread_systems_library.ffm.c.types.unsigned_short
import org.bread_experts_group.bread_systems_library.ffm.util.ArrayElement
import org.bread_experts_group.bread_systems_library.generic.ArraySize
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("PropertyName")
interface GUID : Structure {
    @Order(0) var Data1: unsigned_long
    @Order(1) var Data2: unsigned_short
    @Order(2) var Data3: unsigned_short
    @Order(3) var Data4: @ArraySize(8) ArrayElement<unsigned_char>
}