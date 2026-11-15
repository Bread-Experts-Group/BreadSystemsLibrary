package org.bread_experts_group.bread_systems_library.model.ehr.smart

import org.bread_experts_group.bread_systems_library.generic.io.BlockingInput
import org.bread_experts_group.bread_systems_library.generic.io.FlushingOutput
import org.bread_experts_group.bread_systems_library.generic.io.associateChannel
import org.bread_experts_group.bread_systems_library.model.http.server.*
import org.bread_experts_group.bread_systems_library.model.http.server.request.RequestHandler
import org.bread_experts_group.bread_systems_library.model.http.server.request.SupportsRange
import org.bread_experts_group.bread_systems_library.model.http.server.response.ResponseWriter
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.time.Instant

fun main() {
    val socket = ServerSocketChannel.open()
    socket.bind(InetSocketAddress(80))

    val hosts = ServerHosts().get(
        HostRoute(null, Routable.Path(Routable.Route.Argument)),
        object : RequestHandler(), SupportsRange {
            override fun handle(path: HostRoute, arguments: List<String>, output: ResponseWriter) {
                output
                    .status(200)
                    .writeWithData("Hello ${arguments}".toByteArray())
            }

            override fun handleHeaders(path: HostRoute, arguments: List<String>, output: ResponseWriter): Boolean {
                return true
            }

            override fun handleRange(path: HostRoute, arguments: List<String>, range: HTTPRange, data: (ByteArray) -> Unit) {
                val supported = "Hello $arguments".toByteArray()
                val write = when (range) {
                    is HTTPRange.Suffix -> supported.slice((supported.size - range.end.toInt())..supported.lastIndex)
                    is HTTPRange.ToEnd -> supported.slice(range.start.toInt()..supported.lastIndex)
                    is HTTPRange.Range -> supported.slice(range.start.toInt()..range.end.toInt())
                }
                data(write.toByteArray())
            }

            override fun rangeSatisfiable(
                path: HostRoute,
                arguments: List<String>,
                range: List<HTTPRange>
            ): Boolean {
                val supported = "Hello $arguments".toByteArray()
                return range.all {
                    val start = it.start
                    val end = it.end
                    (start == null || start < supported.size) && (end == null || end < supported.size)
                }
            }
        }
    ).get(
        HostRoute(null, Routable.Path()),
        object : RequestHandler() {
            override fun handle(path: HostRoute, arguments: List<String>, output: ResponseWriter) {
                output
                    .status(200)
                    .writeWithData("${Instant.now()}".toByteArray())
            }
        }
    )

    while (true) {
        val client = socket.accept()
        val input = BlockingInput()

        val output = FlushingOutput.Buffer(ByteBuffer.allocateDirect(4096)) { buffer, length ->
            client.write(buffer.position(length).flip())
        }

        input.associateChannel(client, 4096)
        HTTPServer(input, output, hosts) { client.close() }
    }
}