package test

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.c.types.short
import org.bread_experts_group.bread_systems_library.ffm.c.types.size_t
import org.bread_experts_group.bread_systems_library.ffm.get
import org.bread_experts_group.bread_systems_library.ffm.linux.*
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.SOCK_RAW
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.genlmsghdr
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.nlattr
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.netlink.nlmsghdr
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.pollfd
import org.bread_experts_group.bread_systems_library.ffm.util.layout.NativeArray
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import org.bread_experts_group.bread_systems_library.generic.BlockingInput
import java.lang.foreign.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.typeOf

fun gnlCompute(arena: SegmentAllocator, linker: Linker, data: List<Pair<Short, ByteArray>>): MemorySegment {
    var offset = 0L
    offset += typeOf<nlmsghdr>().computeLayout(linker).byteSize()
    val genlmsghdrOffset = offset
    offset += typeOf<genlmsghdr>().computeLayout(linker).byteSize()
    val nlSize = typeOf<nlattr>().computeLayout(linker).byteSize()
    val attrs = data.map { (_, data) ->
        val sv = offset
        offset += nlSize + data.size
        val remainder = offset % 4
        if (remainder != 0L) offset += 4 - remainder
        sv
    }
    val segment = arena.allocate(offset)

    val nlmsghdr = image<nlmsghdr>(segment)
    nlmsghdr.nlmsg_len = offset.toInt()
    nlmsghdr.nlmsg_type = GENL_ID_CTRL.toShort()
    nlmsghdr.nlmsg_flags = (NLM_F_REQUEST or NLM_F_ACK).toShort()
    nlmsghdr.nlmsg_seq = 1
    val genlmsghdr = image<genlmsghdr>(segment.asSlice(genlmsghdrOffset))
    genlmsghdr.cmd = CTRL_CMD_GETFAMILY.toByte()
    genlmsghdr.version = 2
    data.forEachIndexed { index, (type, data) ->
        val localSlice = segment.asSlice(attrs[index])
        val localnlatatr = image<nlattr>(localSlice)
        localnlatatr.nla_len = (nlSize + data.size).toShort()
        localnlatatr.nla_type = type
        MemorySegment.copy(
            data, 0,
            localSlice.asSlice(nlSize), ValueLayout.JAVA_BYTE, 0L,
            data.size
        )
    }
    return segment
}

fun gnlAttributes(linker: Linker, buf: MemorySegment): Map<Short, ByteBuffer> {
    var offset = 0L
    val msgHeader = image<nlmsghdr>(buf)
    offset += typeOf<nlmsghdr>().computeLayout(linker).byteSize()
    offset += typeOf<genlmsghdr>().computeLayout(linker).byteSize()
    val nlAttrSize = typeOf<nlattr>().computeLayout(linker).byteSize()
    val data = mutableMapOf<Short, ByteBuffer>()
    while (msgHeader.nlmsg_len > offset) {
        val attr = image<nlattr>(buf.asSlice(offset))
        data[attr.nla_type] = buf.asSlice(offset + nlAttrSize)
            .reinterpret(attr.nla_len - nlAttrSize)
            .asByteBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)
        offset += attr.nla_len.toInt() and 0xFFFF
        val remainder = offset % 4
        if (remainder != 0L) offset += (4 - remainder)
    }
    return data
}

fun nl80211Compute(
    familyID: Short, op: Byte, version: Byte, flags: Short,
    arena: SegmentAllocator, linker: Linker, data: List<Pair<Short, ByteArray>>
): MemorySegment {
    var offset = 0L
    offset += typeOf<nlmsghdr>().computeLayout(linker).byteSize()
    val genlmsghdrOffset = offset
    offset += typeOf<genlmsghdr>().computeLayout(linker).byteSize()
    val nlSize = typeOf<nlattr>().computeLayout(linker).byteSize()
    val attrs = data.map { (_, data) ->
        val sv = offset
        offset += nlSize + data.size
        val remainder = offset % 4
        if (remainder != 0L) offset += 4 - remainder
        sv
    }
    val segment = arena.allocate(offset)

    val nlmsghdr = image<nlmsghdr>(segment)
    nlmsghdr.nlmsg_len = offset.toInt()
    nlmsghdr.nlmsg_type = familyID
    nlmsghdr.nlmsg_flags = flags
    nlmsghdr.nlmsg_seq = 1
    val genlmsghdr = image<genlmsghdr>(segment.asSlice(genlmsghdrOffset))
    genlmsghdr.cmd = op
    genlmsghdr.version = version
    data.forEachIndexed { index, (type, data) ->
        val localSlice = segment.asSlice(attrs[index])
        val localnlatatr = image<nlattr>(localSlice)
        localnlatatr.nla_len = (nlSize + data.size).toShort()
        localnlatatr.nla_type = type
        MemorySegment.copy(
            data, 0,
            localSlice.asSlice(nlSize), ValueLayout.JAVA_BYTE, 0L,
            data.size
        )
    }
    return segment
}

class NetLink(libc: libc) {
    init {
        val socket = libc.socket!!(int(AF_NETLINK), int(SOCK_RAW), int(NETLINK_GENERIC))
        val decoder = BlockingInput()
        Thread.ofVirtual().start {
            while (true) {
                val hdr = decoder.readStructure<nlmsghdr>(libc.linker)
                println(hdr)
                when (val type = hdr.nlmsg_type.toInt()) {
                    else -> {
                        decoder.skip(hdr.nlmsg_len - hdr.segment().byteSize().toInt())
                    }

//                    else -> throw NotImplementedError("Message type...$type")
                }
            }
        }
        Thread.ofPlatform().start {
            val pollfds = NativeArray<pollfd>(Arena.global(), 1, libc.linker) { _, pollfd ->
                pollfd.fd = socket
                pollfd.events = short(POLLIN.toShort())
            }
            val recv = Arena.global().allocate(16384)
            while (true) {
                val s = libc.poll!!(Pointer(pollfds), int(1), int(-1))
                if (s.value == -1) TODO("ERR")
                val rx = libc.recv!!(
                    socket,
                    Pointer(typeOf<Void>(), recv), size_t(recv.byteSize()),
                    int(0)
                )
                decoder.load(recv.reinterpret(rx.value as Long).toArray(ValueLayout.JAVA_BYTE))
            }
        }

        val data = gnlCompute(Arena.global(), libc.linker, listOf(CTRL_ATTR_FAMILY_NAME.toShort() to "nl80211\u0000".toByteArray()))
        Thread.sleep(2000)
        libc.send!!(
            socket,
            Pointer(typeOf<Void>(), data), size_t(data.byteSize()),
            int(0)
        )
//        libc.recv!!(
//            socket,
//            Pointer(typeOf<Void>(), recv), size_t(recv.byteSize()),
//            int(0)
//        )
//        val attrs = gnlAttributes(libc.linker, recv)
//
//        val data2 = nl80211Compute(
//            attrs[CTRL_ATTR_FAMILY_ID.toShort()]!!.getShort(), NL80211_CMD_GET_WIPHY.toByte(), 0,
//            (NLM_F_REQUEST or NLM_F_ACK or NLM_F_DUMP).toShort(),
//            Arena.global(), libc.linker, emptyList()
//        )
//        libc.send(
//            socket,
//            Pointer(typeOf<Void>(), data2), size_t(data2.byteSize()),
//            int(0)
//        )
//        val rx2 = libc.recv(
//            socket,
//            Pointer(typeOf<Void>(), recv), size_t(recv.byteSize()),
//            int(0)
//        )
//        println(recv.reinterpret(rx2.value as Long).toArray(ValueLayout.JAVA_BYTE).toHexString())
//        val rx3 = libc.recv(
//            socket,
//            Pointer(typeOf<Void>(), recv), size_t(recv.byteSize()),
//            int(0)
//        )
//        println(recv.reinterpret(rx3.value as Long).toArray(ValueLayout.JAVA_BYTE).toHexString())
    }
}

fun main() {
    val linker = Linker.nativeLinker()
    val libc = get<libc>(SymbolLookup.libraryLookup("libc.so.6", Arena.ofAuto()), linker)
    NetLink(libc)
}