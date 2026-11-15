package org.bread_experts_group.bread_systems_library.model.matrix

import org.bread_experts_group.bread_systems_library.model.codings.IJSONDeserializer
import org.bread_experts_group.bread_systems_library.model.codings.JSONDeserializer
import java.net.InetAddress

@JSONDeserializer(MatrixID.Serializer::class)
class MatrixID(val name: String, val addresses: Array<InetAddress>, val port: UShort? = null) {
    object Serializer : IJSONDeserializer<String, MatrixID> {
        override fun jsonDeserialize(input: String?): MatrixID = MatrixID(input!!)
    }

    operator fun component1(): String = name
    operator fun component2(): Array<InetAddress> = addresses
    operator fun component3(): UShort? = port
}

fun MatrixID(id: String): MatrixID {
    val idSeparator = id.indexOf(':')
    val serverName = id.substring(idSeparator + 1)
    val serverPortSeparator = serverName.lastIndexOf(':')

    val hostName: String
    val serverPort: UShort?
    if (serverPortSeparator != -1) {
        hostName = serverName.take(serverPortSeparator)
        serverPort = serverName.substring(serverPortSeparator + 1).also {
            if (it.any { p -> p !in '0'..'9' }) throw IllegalArgumentException("Server port contains invalid characters")
            if (it.length !in 1..5) throw IllegalArgumentException("Server port not in range")
        }.toUShort()
    } else {
        hostName = serverName
        serverPort = null
    }
    if (hostName.isEmpty()) throw IllegalArgumentException("Host name cannot be empty")
    val hostAddresses = InetAddress.getAllByName(hostName)

    return MatrixID(id.take(idSeparator), hostAddresses, serverPort)
}