package org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface nlattr : Structure {
    @Order(0) var nla_len: Short
    @Order(1) var nla_type: Short
}