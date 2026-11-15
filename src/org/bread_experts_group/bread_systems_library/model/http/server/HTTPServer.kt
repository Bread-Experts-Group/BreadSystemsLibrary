package org.bread_experts_group.bread_systems_library.model.http.server

import org.bread_experts_group.bread_systems_library.generic.io.BlockingInput
import org.bread_experts_group.bread_systems_library.generic.io.FlushingOutput
import org.bread_experts_group.bread_systems_library.model.http.HTTP11Message
import org.bread_experts_group.bread_systems_library.model.http.HTTPMessage
import org.bread_experts_group.bread_systems_library.model.http.httpMessage11
import org.bread_experts_group.bread_systems_library.model.http.server.request.SupportsRange
import org.bread_experts_group.bread_systems_library.model.http.server.response.ResponseWriter
import java.io.IOException
import java.net.URLDecoder

// Compliance: RFC 9112, RFC 9110
class HTTPServer(
    private val input: BlockingInput,
    private val output: FlushingOutput,
    private val hosts: ServerHosts,
    private val closingAction: () -> Unit
) {
    private val utf8 = Charsets.UTF_8.newDecoder()

    init {
        Thread.ofVirtual().start {
            while (true) {
                val message = try {
                    httpMessage11(input, utf8)
                } catch (_: IOException) {
                    closingAction()
                    return@start
                }

                if (message !is HTTP11Message.Request) {
                    HTTP11Message.Response.badRequest(mapOf("Connection" to listOf("close"))).write(output)
                    output.flush()
                    closingAction()
                    return@start
                }

                // TODO: Host needs to be split into name and port, then thats used in routing... port may be null
                // TODO: REquest handler gets a form of message sending with prepopulated headers
                fun handleAll() {
                    val hosts = message.headerFields["host"]
                    if (hosts == null || hosts.size != 1) {
                        HTTP11Message.Response.badRequest(mapOf("Content-Length" to listOf("0"))).write(output)
                        output.flush()
                        return
                    }
                    val host = hosts[0]

                    if (message.method == "CONNECT" || message.method == "OPTIONS") {
                        HTTP11Message.Response.notImplemented(mapOf("Content-Length" to listOf("0"))).write(output)
                        output.flush()
                        return
                    }

                    var routes = this.hosts[host] ?: this.hosts[null]
                    if (routes == null) {
                        HTTP11Message.Response.misdirectedRequest(mapOf("Content-Length" to listOf("0"))).write(output)
                        output.flush()
                        return
                    }

                    val path = HostRoute(
                        host,
                        Routable.Path(
                            URLDecoder.decode(message.requestTarget, Charsets.UTF_8).split('/').mapNotNull {
                                if (it.isEmpty()) null else Routable.Route.Segment(it)
                            }
                        )
                    )

                    val pair = routes.resolve(path)
                    if (pair != null) {
                        val (handlers, arguments) = pair
                        val methodHandler = handlers[message.method]
                        if (methodHandler != null) {
                            fun handle() {
                                val writer = ResponseWriter(this)
                                if (methodHandler is SupportsRange) {
                                    writer.header("accept-ranges", "bytes")
                                    if (message.method == "GET") {
                                        val rangeHeader = message.headerFields["range"]
                                        if (rangeHeader != null && rangeHeader.size == 1) {
                                            val (unit, set) = rangeHeader[0].split('=', limit = 2)
                                            if (unit == "bytes") {
                                                val ranges = set.split(',').map {
                                                    val range = it.trimStart().split('-', limit = 2)
                                                    val r0 = range[0].isEmpty()
                                                    val r1 = range[1].isEmpty()
                                                    if (r0 && r1) throw IllegalArgumentException("Both sides of range invalid")
                                                    if (r0) HTTPRange.Suffix(range[1].toLong())
                                                    else if (r1) HTTPRange.ToEnd(range[0].toLong())
                                                    else HTTPRange.Range(range[0].toLong(), range[1].toLong())
                                                }
                                                // TODO: Sort ranges, if any overlap OR too many, reject with bad request
                                                // TODO: Reject negative values

                                                if (!methodHandler.rangeSatisfiable(path, arguments, ranges)) {
                                                    HTTP11Message.Response.rangeNotSatisfiable(mapOf("Content-Length" to listOf("0"))).write(output)
                                                    output.flush()
                                                    return
                                                }

                                                if (!methodHandler.handleHeaders(path, arguments, writer)) {
                                                    output.flush()
                                                    return
                                                }
                                                writer.status(206)
                                                if (ranges.size > 1) {
                                                    writer.header("content-type", "multipart/byteranges; boundary=***")
                                                    val streamWriter = writer.writeStreaming()
                                                    ranges.forEach { range ->
                                                        methodHandler.handleRange(path, arguments, range) {
                                                            output
                                                            val boundary = "--***\r\ncontent-range: bytes ${range.start ?: ""}-${range.end ?: ""}/*\r\ncontent-type: text/plain\r\n\r\n".toByteArray() + it + "\r\n".toByteArray()
                                                            streamWriter.writeData(boundary)
                                                        }
                                                    }
                                                    streamWriter.writeData("--***--\r\n".toByteArray(Charsets.US_ASCII))
                                                    streamWriter.close()
                                                } else {
                                                    writer.header("content-type", "text/plain")
                                                    methodHandler.handleRange(path, arguments, ranges[0]) {
                                                        writer.writeWithData(it)
                                                    }
                                                }
                                                return
                                            }
                                        }
                                    }

                                } else writer.header("accept-ranges", "none")

                                methodHandler.handle(path, arguments, writer)
                            }

                            handle()
                        } else {
                            HTTP11Message.Response.methodNotAllowed(mapOf("Content-Length" to listOf("0"))).write(output)
                            output.flush()
                        }
                    } else {
                        HTTP11Message.Response.notFound(mapOf("Content-Length" to listOf("0"))).write(output)
                        output.flush()
                    }
                }

                handleAll()
                if (message.headerFields["connection"]?.any { it == "close" } == true) {
                    closingAction()
                    return@start
                }
            }
        }
    }

    private var streaming = false
    internal fun writeResponse(responseWriter: ResponseWriter, data: ByteArray?) {
        if (data != null) responseWriter.header("content-length", data.size.toString())
        else {
            responseWriter.header("transfer-encoding", "chunked")
            streaming = true
        }
        HTTP11Message.Response(
            HTTPMessage.Version.HTTP_1_1,
            responseWriter.headers, responseWriter.status!!,
            responseWriter.h1ReasonPhrase
        ).write(output)
        if (data != null) output.writeData(data)
        output.flush()
    }

    internal fun writeData(data: ByteArray, finish: Boolean) {
        if (!streaming) throw IllegalStateException("Not data streaming")
        if (data.isNotEmpty()) {
            output.writeData("${data.size.toString(16)}\r\n".toByteArray(Charsets.US_ASCII))
            output.writeData(data)
            output.writeData("\r\n".toByteArray(Charsets.US_ASCII))
        }
        if (finish) {
            output.writeData("0\r\n\r\n".toByteArray(Charsets.US_ASCII))
            output.flush()
            streaming = false
        }
    }
}