package router

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
import org.bread_experts_group.bread_systems_library.generic.BlockingInput
import org.bread_experts_group.bread_systems_library.generic.FlushingOutput
import java.lang.foreign.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates
import kotlin.reflect.typeOf

class GenericNetLink(private val libc: libc) {
    private val socket = libc.socket!!(
        int(AF_NETLINK),
        int(SOCK_RAW),
        int(NETLINK_GENERIC)
    )

    private val output = FlushingOutput(Arena.global().allocate(16384)) { buffer, offset ->
        libc.send!!(
            socket,
            Pointer(typeOf<Void>(), buffer), size_t(offset),
            int(0)
        )
    }

    private var messageCounter = 0
    private val messages = mutableMapOf<Int, Pair<MutableList<GenericNetlinkMessage>?, CompletableFuture<List<GenericNetlinkMessage>>>>()

    fun send(
        type: Short = GENL_ID_CTRL.toShort(), cmd: Byte, flags: UShort = 0u,
        data: List<Pair<Short, ByteArray>>
    ): CompletableFuture<List<GenericNetlinkMessage>> {
        val future = CompletableFuture<List<GenericNetlinkMessage>>()
        val seq = messageCounter++
        messages[seq] = (if (flags.toInt() and NLM_F_DUMP != 0) mutableListOf<GenericNetlinkMessage>() else null) to future

        var offset = 0L
        offset += typeOf<nlmsghdr>().computeLayout(libc.linker).byteSize()
        offset += typeOf<genlmsghdr>().computeLayout(libc.linker).byteSize()
        val nlSize = typeOf<nlattr>().computeLayout(libc.linker).byteSize()
        data.forEach { (_, data) ->
            offset += nlSize + data.size
            val remainder = offset % 4
            if (remainder != 0L) offset += 4 - remainder
        }
        output.writeStructure<nlmsghdr>(libc.linker) {
            nlmsg_len = offset.toInt()
            nlmsg_type = type
            nlmsg_flags = (NLM_F_REQUEST or flags.toInt()).toShort()
            nlmsg_seq = seq
        }
        output.writeStructure<genlmsghdr>(libc.linker) {
            this.cmd = cmd
            version = 2
        }

        data.forEach { (type, data) ->
            output.writeStructure<nlattr>(libc.linker) {
                nla_len = (nlSize + data.size).toShort()
                nla_type = type
            }
            output.writeData(data)
            output.align(4)
        }
        output.flush()
        return future
    }

    init {
        val decoder = BlockingInput()
        Thread.ofVirtual().start {
            while (true) {
                decoder.reads = 0
                val hdr = decoder.readStructure<nlmsghdr>(libc.linker)
                when (hdr.nlmsg_type.toInt()) {
                    NLMSG_NOOP -> {
                        decoder.skip(hdr.nlmsg_len - hdr.segment().byteSize().toInt())
                    }

                    NLMSG_ERROR -> {
                        println(decoder.readInt())
                        decoder.skip(hdr.nlmsg_len - hdr.segment().byteSize().toInt() - 4)
                        messages[hdr.nlmsg_seq]!!.second.complete(emptyList())
                    }

                    NLMSG_OVERRUN -> TODO("!")

                    NLMSG_DONE -> {
                        val work = messages.remove(hdr.nlmsg_seq)
                        work?.second?.complete(work.first!!)
                        decoder.skip(hdr.nlmsg_len - hdr.segment().byteSize().toInt())
                    }

                    else -> {
                        val glHdr = decoder.readStructure<genlmsghdr>(libc.linker)
                        val attrs = buildList {
                            while (hdr.nlmsg_len > decoder.reads) {
                                val attr = decoder.readStructure<nlattr>(libc.linker)
                                add(
                                    NetLinkAttribute(
                                        attr,
                                        decoder.readBytes((attr.nla_len.toInt() and 0xFFFF) - attr.segment().byteSize().toInt())
                                    )
                                )
                                decoder.align(4)
                            }
                        }
                        val work = messages[hdr.nlmsg_seq]
                        if (work != null) {
                            val message = GenericNetlinkMessage(hdr, glHdr, attrs)
                            val list = work.first
                            if (list != null) list.addLast(message)
                            else {
                                messages.remove(hdr.nlmsg_seq)
                                work.second.complete(listOf(message))
                            }
                        }
                    }
                }
            }
        }
        Thread.ofPlatform().daemon().start {
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
    }
}

fun main() {
    val linker = Linker.nativeLinker()
    val libc = get<libc>(SymbolLookup.libraryLookup("libc.so.6", Arena.ofAuto()), linker)
    val gnl = GenericNetLink(libc)

    val nl = gnl.send(
        GENL_ID_CTRL.toShort(), CTRL_CMD_GETFAMILY.toByte(), 0u,
        listOf(CTRL_ATTR_FAMILY_NAME.toShort() to "nl80211\u0000".toByteArray())
    ).join()
    val nlFamilyID = MemorySegment.ofArray(
        nl.first().attributes.first { it.structure.nla_type == CTRL_ATTR_FAMILY_ID.toShort() }.data
    ).get(ValueLayout.JAVA_SHORT_UNALIGNED, 0)
    val nlData = gnl.send(
        nlFamilyID, NL80211_CMD_GET_WIPHY.toByte(), (NLM_F_DUMP).toUShort(),
        listOf(NL80211_ATTR_SPLIT_WIPHY_DUMP.toShort() to byteArrayOf())
    ).join()

    fun attributes(attribute: NetLinkAttribute): Iterable<NetLinkAttribute> = object : Iterable<NetLinkAttribute> {
        override fun iterator(): Iterator<NetLinkAttribute> = object : Iterator<NetLinkAttribute> {
            val input = BlockingInput()

            init {
                input.load(attribute.data)
            }

            override fun next(): NetLinkAttribute {
                val structure = input.readStructure<nlattr>(libc.linker)
                val data = input.readBytes(((structure.nla_len.toInt() and 0xFFFF) - structure.segment().byteSize()).toInt())
                input.align(4)
                return NetLinkAttribute(structure, data)
            }

            override fun hasNext(): Boolean = input.reads < attribute.data.size
        }
    }

    val wirelessPhysicalInterfacesC = mutableMapOf<Int, WirelessPhysicalInterface.Construction>()
    nlData.forEach { data ->
        var wiphyConstruction by Delegates.notNull<WirelessPhysicalInterface.Construction>()
        data.attributes.forEach {
            when (it.structure.nla_type) {
                NL80211_ATTR_WIPHY.toShort() -> {
                    wiphyConstruction = wirelessPhysicalInterfacesC.getOrPut(
                        MemorySegment.ofArray(it.data).get(ValueLayout.JAVA_INT_UNALIGNED, 0)
                    ) { WirelessPhysicalInterface.Construction() }
                }

                NL80211_ATTR_WIPHY_NAME.toShort() -> {
                    wiphyConstruction.name = MemorySegment.ofArray(it.data).getString(0, Charsets.UTF_8)
                }

                NL80211_ATTR_WIPHY_BANDS.toShort() -> {
                    attributes(it).forEach { band ->
                        val bandConstruction = wiphyConstruction.bands.getOrPut(band.structure.nla_type.toUShort()) {
                            WirelessPhysicalInterface.Band.Construction()
                        }
                        attributes(band).forEachIndexed { _, bandAttribute ->
                            when (bandAttribute.structure.nla_type) {
                                NL80211_BAND_ATTR_FREQS.toShort() -> {
                                    attributes(bandAttribute).forEach { frequency ->
                                        val freqConstruction = bandConstruction.frequencies.getOrPut(frequency.structure.nla_type.toUShort()) {
                                            WirelessPhysicalInterface.Band.Frequency.Construction()
                                        }
                                        attributes(frequency).forEach { frequencyAttribute ->
                                            val segment = MemorySegment.ofArray(frequencyAttribute.data)
                                            when (frequencyAttribute.structure.nla_type) {
                                                NL80211_FREQUENCY_ATTR_FREQ.toShort() -> freqConstruction.frequencyHz = (
                                                        segment.get(ValueLayout.JAVA_INT_UNALIGNED, 0).toLong() and 0xFFFFFFFF
                                                ) * 1_000_000
//                                                NL80211_FREQUENCY_ATTR_DISABLED.toShort() -> println(
//                                                    "   Disabled (regulatory)"
//                                                )
//                                                NL80211_FREQUENCY_ATTR_NO_IR.toShort() -> println(
//                                                    "   No emitted radiation (regulatory)"
//                                                )
//                                                NL80211_FREQUENCY_ATTR_NO_IBSS.toShort() -> println(
//                                                    "   IBSS networks disabled (regulatory)"
//                                                )
//                                                NL80211_FREQUENCY_ATTR_RADAR.toShort() -> println(
//                                                    "   Radar detection (regulatory)"
//                                                )
//                                                NL80211_FREQUENCY_ATTR_MAX_TX_POWER.toShort() -> println(
//                                                    "   Max TX ${MemorySegment.ofArray(frequencyAttribute.data).get(ValueLayout.JAVA_INT_UNALIGNED, 0)} mBm"
//                                                )
//                                                NL80211_FREQUENCY_ATTR_OFFSET.toShort() -> println(
//                                                    "   Frequency offset ${MemorySegment.ofArray(frequencyAttribute.data).get(ValueLayout.JAVA_INT_UNALIGNED, 0)} kHz"
//                                                )

                                                else -> freqConstruction.attributes.add(frequencyAttribute)
                                            }
                                        }
                                    }
                                }

                                NL80211_BAND_ATTR_RATES.toShort() -> {
                                    attributes(bandAttribute).forEach { rate ->
                                        val freqConstruction = bandConstruction.rates.getOrPut(rate.structure.nla_type.toUShort()) {
                                            WirelessPhysicalInterface.Band.Rate.Construction()
                                        }
                                        attributes(rate).forEachIndexed { _, frequencyAttribute ->
                                            when (frequencyAttribute.structure.nla_type) {
//                                                NL80211_BITRATE_ATTR_RATE.toShort() -> println(
//                                                    "   ${MemorySegment.ofArray(frequencyAttribute.data).get(ValueLayout.JAVA_INT_UNALIGNED, 0) * 100} Kbps"
//                                                )

                                                else -> freqConstruction.attributes.add(frequencyAttribute)
                                            }
                                        }
                                    }
                                }

                                else -> bandConstruction.attributes.add(bandAttribute)
                            }
                        }
                    }
                }
                else -> wiphyConstruction.attributes.add(it)
            }
        }
    }
    val wirelessPhysicalInterfaces = Array(wirelessPhysicalInterfacesC.keys.max() + 1) {
        wirelessPhysicalInterfacesC[it]!! as WirelessPhysicalInterface
    }

    val ifIndex = ByteBuffer.allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(5)
        .array()
    val NL80211_IFTYPE_AP = 3
    gnl.send(
        nlFamilyID,
        NL80211_CMD_SET_INTERFACE.toByte(),
        NLM_F_ACK.toUShort(),
        listOf(
            NL80211_ATTR_IFINDEX.toShort() to ifIndex,
            NL80211_ATTR_IFTYPE.toShort() to ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(NL80211_IFTYPE_AP)
                .array()
        )
    ).thenAccept {
        println(it)
    }.join()

    gnl.send(
        nlFamilyID,
        NL80211_CMD_SET_WIPHY.toByte(),
        NLM_F_ACK.toUShort(),
        listOf(
            NL80211_ATTR_IFINDEX.toShort() to ifIndex,
            NL80211_ATTR_WIPHY_FREQ.toShort() to ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(2412)
                .array()
        )
    ).thenAccept {
        println(it)
    }.join()

    // TODO:
    // NL80211_ATTR_BEACON_INTERVAL (u32, TU (1024 microseconds))
    // NL80211_ATTR_DTIM_PERIOD (u32)
    // NL80211_ATTR_BEACON_HEAD (binary)
    val leInt: (Int) -> ByteArray = {
        ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(it)
            .array()
    }

    gnl.send(
        nlFamilyID,
        NL80211_CMD_STOP_AP.toByte(),
        NLM_F_ACK.toUShort(),
        listOf(
            NL80211_ATTR_SSID.toShort() to "test".toByteArray(Charsets.UTF_8),
            NL80211_ATTR_IFINDEX.toShort() to leInt(5),
            NL80211_ATTR_BEACON_INTERVAL.toShort() to leInt(100),
            NL80211_ATTR_DTIM_PERIOD.toShort() to leInt(1),
            NL80211_ATTR_BEACON_HEAD.toShort() to
                    "80000000ffffffffffff0ccdb46cf3bb0ccdb46cf3bb0000000000000000000064000100"
                        .hexToByteArray(),
            NL80211_ATTR_BEACON_TAIL.toShort() to
                    byteArrayOf(0x05, 0x04, 0x00, 0x01, 0x00, 0x00)
        )
    ).thenAccept(::println).join()

    gnl.send(
        nlFamilyID,
        NL80211_CMD_START_AP.toByte(),
        NLM_F_ACK.toUShort(),
        listOf(
            NL80211_ATTR_IFINDEX.toShort() to leInt(5),
            NL80211_ATTR_BEACON_INTERVAL.toShort() to leInt(100),
            NL80211_ATTR_DTIM_PERIOD.toShort() to leInt(1),
            NL80211_ATTR_BEACON_HEAD.toShort() to
                    "80000000ffffffffffff0ccdb46cf3bb0ccdb46cf3bb00000000000000000000000064000100000474657374010482848b96030101"
                        .hexToByteArray(),
            NL80211_ATTR_BEACON_TAIL.toShort() to
                    byteArrayOf(0x05, 0x04, 0x00, 0x01, 0x00, 0x00)
        )
    ).thenAccept(::println).join()
    Thread.sleep(99999999)
}