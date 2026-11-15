package org.bread_experts_group.bread_systems_library.model.http

import org.bread_experts_group.bread_systems_library.generic.io.BlockingInput
import org.bread_experts_group.bread_systems_library.generic.io.FlushingOutput
import org.bread_experts_group.bread_systems_library.model.channels.EmptyChannel
import org.bread_experts_group.bread_systems_library.model.http.HTTPMessage.Version.Companion.HTTP_1_1
import java.nio.channels.ReadableByteChannel
import java.nio.charset.CharsetDecoder
import java.util.*

sealed interface HTTP11Message : HTTPMessage {
    companion object {
        protected fun HTTP11Message.writeHeadersAndBody11(output: FlushingOutput) {
            headerFields.forEach { (key, values) ->
                values.forEach { value ->
                    output.writeData("$key: $value\r\n".toByteArray(Charsets.US_ASCII))
                }
            }
            output.writeData("\r\n".toByteArray(Charsets.US_ASCII))
            output.writeData(body)
        }
    }

    data class Request(
        override val version: HTTPMessage.Version, override val headerFields: Map<String, List<String>>,
        override val method: String, override val requestTarget: String
    ) : HTTPMessage.Request(HTTPMessage.Version.HTTP_1_1, headerFields, method, requestTarget), HTTP11Message {
        override var body: ReadableByteChannel = EmptyChannel()
            set(value) {
                if (field !is EmptyChannel) throw IllegalStateException("Body already initialized")
                field = value
            }

        override fun write(output: FlushingOutput) {
            output.writeData(
                "$method $requestTarget HTTP/${version.major}.${version.minor}\r\n".toByteArray(Charsets.US_ASCII)
            )
            writeHeadersAndBody11(output)
        }
    }

    data class Response(
        override val version: HTTPMessage.Version, override val headerFields: Map<String, List<String>>,
        override val statusCode: Int, val reasonPhrase: String = statusMessages[statusCode] ?: ""
    ) : HTTPMessage.Response(version, headerFields, statusCode), HTTP11Message {
        constructor(
            headerFields: Map<String, List<String>>,
            statusCode: Int, reasonPhrase: String = statusMessages[statusCode] ?: ""
        ) : this(HTTP_1_1, headerFields, statusCode, reasonPhrase)

        override var body: ReadableByteChannel = EmptyChannel()
            set(value) {
                if (field !is EmptyChannel) throw IllegalStateException("Body already initialized")
                field = value
            }

        override fun write(output: FlushingOutput) {
            output.writeData(
                "HTTP/${version.major}.${version.minor} $statusCode $reasonPhrase\r\n".toByteArray(Charsets.US_ASCII)
            )
            writeHeadersAndBody11(output)
        }

        @Suppress("unused")
        companion object {
            val statusMessages = mapOf(
                100 to "Continue",
                101 to "Switching Protocols",

                200 to "OK",
                201 to "Created",
                202 to "Accepted",
                203 to "Non-Authoritative Information",
                204 to "No Content",
                205 to "Reset Content",
                206 to "Partial Content",

                300 to "Multiple Choices",
                301 to "Moved Permanently",
                302 to "Found",
                303 to "See Other",
                304 to "Not Modified",
                305 to "Use Proxy",
                307 to "Temporary Redirect",
                308 to "Permanent Redirect",

                400 to "Bad Request",
                401 to "Unauthorized",
                402 to "Payment Required",
                403 to "Forbidden",
                404 to "Not Found",
                405 to "Method Not Allowed",
                406 to "Not Acceptable",
                407 to "Proxy Authentication Required",
                408 to "Request Timeout",
                409 to "Conflict",
                410 to "Gone",
                411 to "Length Required",
                412 to "Precondition Failed",
                413 to "Content Too Large",
                414 to "URI Too Long",
                415 to "Unsupported Media Type",
                416 to "Range Not Satisfiable",
                417 to "Expectation Failed",
                421 to "Misdirected Request",
                422 to "Unprocessable Content",
                426 to "Upgrade Required",

                500 to "Internal Server Error",
                501 to "Not Implemented",
                502 to "Bad Gateway",
                503 to "Service Unavailable",
                504 to "Gateway Timeout",
                505 to "HTTP Version Not Supported"
            )

            @Suppress("FunctionName")
            fun continue_(headerFields: Map<String, List<String>>) = Response(headerFields, 100)
            fun switchingProtocols(headerFields: Map<String, List<String>>) = Response(headerFields, 101)

            fun ok(headerFields: Map<String, List<String>>) = Response(headerFields, 200)
            fun created(headerFields: Map<String, List<String>>) = Response(headerFields, 201)
            fun accepted(headerFields: Map<String, List<String>>) = Response(headerFields, 202)
            fun nonAuthoritativeInformation(headerFields: Map<String, List<String>>) = Response(headerFields, 203)
            fun noContent(headerFields: Map<String, List<String>>) = Response(headerFields, 204)
            fun resetContent(headerFields: Map<String, List<String>>) = Response(headerFields, 205)
            fun partialContent(headerFields: Map<String, List<String>>) = Response(headerFields, 206)

            fun multipleChoices(headerFields: Map<String, List<String>>) = Response(headerFields, 300)
            fun movedPermanently(headerFields: Map<String, List<String>>) = Response(headerFields, 301)
            fun found(headerFields: Map<String, List<String>>) = Response(headerFields, 302)
            fun seeOther(headerFields: Map<String, List<String>>) = Response(headerFields, 303)
            fun notModified(headerFields: Map<String, List<String>>) = Response(headerFields, 304)
            fun useProxy(headerFields: Map<String, List<String>>) = Response(headerFields, 305)
            fun temporaryRedirect(headerFields: Map<String, List<String>>) = Response(headerFields, 307)
            fun permanentRedirect(headerFields: Map<String, List<String>>) = Response(headerFields, 308)

            fun badRequest(headerFields: Map<String, List<String>>) = Response(headerFields, 400)
            fun unauthorized(headerFields: Map<String, List<String>>) = Response(headerFields, 401)
            fun paymentRequired(headerFields: Map<String, List<String>>) = Response(headerFields, 402)
            fun forbidden(headerFields: Map<String, List<String>>) = Response(headerFields, 403)
            fun notFound(headerFields: Map<String, List<String>>) = Response(headerFields, 404)
            fun methodNotAllowed(headerFields: Map<String, List<String>>) = Response(headerFields, 405)
            fun notAcceptable(headerFields: Map<String, List<String>>) = Response(headerFields, 406)
            fun proxyAuthenticationRequired(headerFields: Map<String, List<String>>) = Response(headerFields, 407)
            fun requestTimeout(headerFields: Map<String, List<String>>) = Response(headerFields, 408)
            fun conflict(headerFields: Map<String, List<String>>) = Response(headerFields, 409)
            fun gone(headerFields: Map<String, List<String>>) = Response(headerFields, 410)
            fun lengthRequired(headerFields: Map<String, List<String>>) = Response(headerFields, 411)
            fun preconditionFailed(headerFields: Map<String, List<String>>) = Response(headerFields, 412)
            fun contentTooLarge(headerFields: Map<String, List<String>>) = Response(headerFields, 413)
            fun uriTooLong(headerFields: Map<String, List<String>>) = Response(headerFields, 414)
            fun unsupportedMediaType(headerFields: Map<String, List<String>>) = Response(headerFields, 415)
            fun rangeNotSatisfiable(headerFields: Map<String, List<String>>) = Response(headerFields, 416)
            fun expectationFailed(headerFields: Map<String, List<String>>) = Response(headerFields, 417)
            fun misdirectedRequest(headerFields: Map<String, List<String>>) = Response(headerFields, 421)
            fun unprocessableContent(headerFields: Map<String, List<String>>) = Response(headerFields, 422)
            fun upgradeRequired(headerFields: Map<String, List<String>>) = Response(headerFields, 426)

            fun internalServerError(headerFields: Map<String, List<String>>) = Response(headerFields, 500)
            fun notImplemented(headerFields: Map<String, List<String>>) = Response(headerFields, 501)
            fun badGateway(headerFields: Map<String, List<String>>) = Response(headerFields, 502)
            fun serviceUnavailable(headerFields: Map<String, List<String>>) = Response(headerFields, 503)
            fun gatewayTimeout(headerFields: Map<String, List<String>>) = Response(headerFields, 504)
            fun httpVersionNotSupported(headerFields: Map<String, List<String>>) = Response(headerFields, 505)
        }
    }

    fun write(output: FlushingOutput)
}


fun String.indexOfLastBackwards(op: (Char) -> Boolean): Int {
    var index = this.lastIndex
    while (index > -1) {
        if (op(this[index])) return index
        index--
    }
    return -1
}

fun httpMessage11(input: BlockingInput, decoder: CharsetDecoder): HTTPMessage {
    var startLine: String
    while (true) {
        startLine = input.readUntil("\n", decoder)
        if (startLine != "\r") break
    }
    startLine = startLine.removeSuffix("\r")
    if (startLine.contains("\r")) throw IllegalArgumentException("Bare CR in message ($startLine)")

    fun parseVersion(version: String): HTTPMessage.Version {
        val d1 = version[5]
        val d2 = version[7]
        if (
            version[4] != '/' || version[6] != '.' || d1.code !in 0x30..0x39 || d2.code !in 0x30..0x39
        ) throw IllegalArgumentException("Bad HTTP version ($startLine)")
        return HTTPMessage.Version(d1.digitToInt(), d2.digitToInt())
    }

    val headerFields = mutableMapOf<String, List<String>>()
    val message = if (startLine.startsWith("HTTP")) {
        if (startLine.length < 14) throw IllegalArgumentException("Message status line under required size ($startLine)")
        if (startLine[9] != ' ' || startLine[13] != ' ') throw IllegalArgumentException("Malformed message status line ($startLine)")
        val version = parseVersion(startLine.substring(0..7))
        val statusCode = startLine.substring(10..12).let {
            if (!it.all { c -> c.code in 0x30..0x39 }) throw IllegalArgumentException(
                "Message status code is not 3 ASCII digits ($startLine)"
            )
            it.toInt()
        }
        val reasonPhrase = startLine.substring(13)
        if (!reasonPhrase.all {
                it.code in 0x21..0x7E || it.code in 0x80..0xFF || it == ' ' || it == '\t'
            }) throw IllegalArgumentException("Bad reason phrase ($reasonPhrase)")
        HTTP11Message.Response(version, headerFields, statusCode, reasonPhrase)
    } else {
        val separator1 = startLine.indexOf(' ')
        if (separator1 == -1) throw IllegalArgumentException("Missing method/request URI separator in request line ($startLine)")
        val separator2 = startLine.indexOfLastBackwards { it == ' ' }
        if (separator2 == separator1) throw IllegalArgumentException("Missing request URI/version separator in request line ($startLine)")

        val version = parseVersion(startLine.substring(separator2 + 1))
        val requestTarget = startLine.substring(separator1 + 1..<separator2)
        val method = startLine.take(separator1)
        if (
            method.isEmpty() || !method.all {
                it in "!#$%&'*+-.^_`|~" || it.code in 0x41..0x5A || it.code in 0x61..0x7A ||
                        it.code in 0x30..0x39
            }
        ) throw IllegalArgumentException("Bad method ($method)")
        HTTP11Message.Request(version, headerFields, method, requestTarget)
    }

    while (true) {
        val fieldLine = input.readUntil("\n", decoder).removeSuffix("\r")
        if (fieldLine.contains("\r"))
            throw IllegalArgumentException("Bare CR in message ($fieldLine)")
        if (fieldLine.isEmpty()) break
        if (fieldLine[0].let { it == ' ' || it == '\t' })
            throw IllegalArgumentException("Whitespace before field line ($fieldLine)")

        val separator = fieldLine.indexOf(':')
        if (separator == -1) throw IllegalArgumentException("Header has no separator ($fieldLine)")
        val name = fieldLine.take(separator)
        if (
            name.isEmpty() || !name.all {
                it in "!#$%&'*+-.^_`|~" || it.code in 0x41..0x5A || it.code in 0x61..0x7A ||
                        it.code in 0x30..0x39
            }
        ) throw IllegalArgumentException("Bad header name ($name)")

        val value = fieldLine.substring(separator + 1).run {
            val start = indexOfFirst { it != '\t' && it != ' ' }
            if (start != -1) substring(start, indexOfLastBackwards { it != '\t' && it != ' ' } + 1) else ""
        }
        if (!value.all {
                it.code in 0x21..0x7E || it.code in 0x80..0xFF || it == ' ' || it == '\t'
            }) throw IllegalArgumentException("Bad header value ($value)")

        (headerFields.getOrPut(name.lowercase(Locale.ROOT)) { mutableListOf() } as MutableList<String>).add(value)
    }

//    message.validate()
    return message
}