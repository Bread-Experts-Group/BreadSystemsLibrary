package org.bread_experts_group.bread_systems_library.model.channels

import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.ReadableByteChannel

class EmptyChannel : ReadableByteChannel {
    private var closed = false
    override fun close() {
        closed = true
    }

    override fun isOpen(): Boolean = true

    override fun read(dst: ByteBuffer?): Int {
        if (closed) throw ClosedChannelException()
        return -1
    }
}