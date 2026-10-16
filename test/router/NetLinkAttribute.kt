package router

import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.nlattr

data class NetLinkAttribute(
    val structure: nlattr,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetLinkAttribute

        if (structure != other.structure) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structure.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}