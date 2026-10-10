package org.bread_experts_group.bread_systems_library.ffm.linux.types.structs

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.c.types.short
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface pollfd : Structure {
    @Order(0) var fd: int
    @Order(1) var events: short
    @Order(2) var revents: short
}