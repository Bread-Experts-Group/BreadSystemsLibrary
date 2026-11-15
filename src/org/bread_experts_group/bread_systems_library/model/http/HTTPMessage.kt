package org.bread_experts_group.bread_systems_library.model.http

import java.nio.channels.ReadableByteChannel

interface HTTPMessage {
    val version: Version
    val headerFields: Map<String, List<String>>

    var body: ReadableByteChannel

    data class Version(
        val major: Int,
        val minor: Int
    ) {
        companion object {
            val HTTP_1_1 = Version(1, 1)
            val HTTP_2 = Version(2, 0)
            val HTTP_3 = Version(3, 0)
        }
    }

    sealed class Request(
        override val version: Version, override val headerFields: Map<String, List<String>>,
        open val method: String, open val requestTarget: String
    ) : HTTPMessage

    sealed class Response(
        override val version: Version, override val headerFields: Map<String, List<String>>,
        open val statusCode: Int
    ) : HTTPMessage
}