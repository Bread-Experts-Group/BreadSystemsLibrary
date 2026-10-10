package org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface genlmsghdr : Structure {
    @Order(0) var cmd: Byte
    @Order(1) var version: Byte
    @Order(2) var reserved: Short
}