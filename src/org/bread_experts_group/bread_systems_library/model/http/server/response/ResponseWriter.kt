package org.bread_experts_group.bread_systems_library.model.http.server.response

import org.bread_experts_group.bread_systems_library.model.http.HTTP11Message.Response.Companion.statusMessages
import org.bread_experts_group.bread_systems_library.model.http.server.HTTPServer

class ResponseWriter(private val server: HTTPServer) {
    internal val headers = mutableMapOf<String, MutableList<String>>()
    private var headersSent = false
    private fun checkHeadersSent() {
        if (headersSent) throw IllegalStateException("Headers already sent")
    }

    internal var status: Int? = null
    internal var h1ReasonPhrase: String = ""

    fun status(status: Int, h1ReasonPhrase: String = statusMessages[status] ?: ""): ResponseWriter {
        this.status = status
        this.h1ReasonPhrase = h1ReasonPhrase
        return this
    }

    fun header(name: String, value: String): ResponseWriter {
        checkHeadersSent()
        headers.getOrPut(name) { mutableListOf() }.add(value)
        return this
    }

    private fun checkStatus() {
        if (status == null || status !in 0..999) throw IllegalArgumentException("Status must be between 0 and 999")
    }

    fun writeWithData(data: ByteArray) {
        checkStatus()
        checkHeadersSent()
        server.writeResponse(this, data)
        headersSent = true
    }

    inner class StreamWriter internal constructor() : AutoCloseable {
        fun writeData(data: ByteArray) {
            server.writeData(data, false)
        }

        override fun close() {
            server.writeData(byteArrayOf(), true)
        }
    }

    fun writeStreaming(): StreamWriter {
        checkStatus()
        checkHeadersSent()
        server.writeResponse(this, null)
        headersSent = true
        return StreamWriter()
    }
}