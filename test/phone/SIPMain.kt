package phone

import org.bread_experts_group.bread_systems_library.generic.BlockingInput
import java.io.EOFException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

data class SIPRequest(
    val method: String,
    val requestURI: String,
    val sipVersion: String,
    val headers: Map<String, String>,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SIPRequest

        if (method != other.method) return false
        if (requestURI != other.requestURI) return false
        if (sipVersion != other.sipVersion) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + requestURI.hashCode()
        result = 31 * result + sipVersion.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

val utf8 = Charsets.UTF_8.newDecoder()
fun SIPRequest(input: BlockingInput): SIPRequest {
    val method = input.readUntil(" ", utf8)
    val requestURI = input.readUntil(" ", utf8)
    val sipVersion = input.readUntil("\r\n", utf8)
    val headers = buildMap {
        while (true) {
            val messageHeader = input.readUntil("\r\n", utf8)
            if (messageHeader.isEmpty()) break
            val colon = messageHeader.indexOf(':')
            put(messageHeader.take(colon).trimEnd(), messageHeader.substring(colon + 1).trimStart())
        }
    }
    return SIPRequest(
        method, requestURI, sipVersion,
        headers, headers["Content-Length"]?.let {
            input.readBytes(it.toInt())
        } ?: ByteArray(0)
    )
}

fun main() {
    val server = ServerSocketChannel.open()
    server.bind(InetSocketAddress(5060))
    while (true) {
        val socket = server.accept()
        val buffer = ByteBuffer.allocate(32768)
        val input = BlockingInput()
        input.suspensionNotification = {
            val read = socket.read(buffer.rewind())
            if (read == -1) throw EOFException()
            val data = ByteArray(read)
            buffer.rewind().get(data)
            input.load(data)
        }
        Thread.ofVirtual().start {
            while (true) {
                println(SIPRequest(input))
            }
        }
    }
}