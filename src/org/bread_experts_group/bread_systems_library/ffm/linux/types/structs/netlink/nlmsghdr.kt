package org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface nlmsghdr : Structure {
    @Order(0) var nlmsg_len: Int
    @Order(1) var nlmsg_type: Short
    @Order(2) var nlmsg_flags: Short
    @Order(3) var nlmsg_seq: Int
    @Order(4) var nlmsg_pid: Int
}