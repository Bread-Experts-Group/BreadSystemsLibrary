package router

import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.genlmsghdr
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.nlmsghdr

data class GenericNetlinkMessage(
    val header: nlmsghdr,
    val gNLHeader: genlmsghdr,
    val attributes: List<NetLinkAttribute>
)