package org.bread_experts_group.bread_systems_library.generic

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.min
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class BlockingInput {
    private val lock = ReentrantReadWriteLock()
    private val rCondition = lock.writeLock().newCondition()
    private val data = mutableListOf<ByteBuffer>()

    var suspensionNotification: ((required: Int) -> Unit)? = null

    var reads = 0L

    private fun hasEnough(space: Int): Int {
        if (space == 0) return 0
        if (data.isEmpty()) return space
        var count = 0
        for (slice in data) {
            count += slice.remaining()
            if (count >= space) return 0
        }
        return space - count
    }

    private fun suspend(space: Int) {
        while (true) {
            val required = hasEnough(space)
            if (required == 0) return
            lock.write {
                suspensionNotification?.invoke(required)
                if (hasEnough(space) == 0) return
                rCondition.await()
            }
        }
    }

    fun readByte(): Byte = lock.read {
        suspend(Byte.SIZE_BYTES)
        val available = data.first()
        val value = available.get()
        reads++
        if (!available.hasRemaining()) data.removeFirst()
        return value
    }

    private val internalBuffer = ByteBuffer.allocate(8)
    var endian: ByteOrder
        get() = internalBuffer.order()
        set(value) {
            internalBuffer.order(value)
        }

    private val charBuffer = CharBuffer.allocate(1)
    fun readChar(decoder: CharsetDecoder): Char {
        internalBuffer.limit(1)
        while (true) {
            readBytes(internalBuffer.rewind(), 1)
            when (val result = decoder.decode(internalBuffer.rewind(), charBuffer.rewind(), false)) {
                CoderResult.UNDERFLOW -> {
                    if (!charBuffer.hasRemaining()) return charBuffer.rewind().get()
                }

                else -> result.throwException()
            }
        }
    }

    fun readUntil(string: String, decoder: CharsetDecoder): String {
        val builder = StringBuilder()
        while (true) {
            builder.append(readChar(decoder))
            if (builder.endsWith(string)) {
                builder.setLength(builder.length - string.length)
                return builder.toString()
            }
        }
    }

    fun readInt(): Int = lock.read {
        readBytes(internalBuffer.clear(), 4)
        return internalBuffer.flip().getInt()
    }

    fun readBytes(length: Int): ByteArray = lock.read {
        var size = length
        suspend(size)
        val structData = ByteArray(size)
        var offset = 0
        while (size > 0) {
            val buffer = data.first()
            val length = min(buffer.remaining(), size)
            buffer.get(structData, offset, length)
            reads += length
            if (!buffer.hasRemaining()) data.removeFirst()
            offset += length
            size -= length
        }
        return structData
    }

    fun readBytes(buffer: ByteBuffer, minimumRead: Int = 0): Unit = lock.read {
        if (minimumRead > buffer.remaining()) throw IllegalArgumentException("Minimum read too large, cannot fit in destination buffer")
        if (minimumRead == 0 && data.isEmpty()) return
        suspend(minimumRead)

        var offset = 0
        var minimumLeft = if (minimumRead == 0) buffer.remaining() else minimumRead
        while (minimumLeft > 0) {
            val srcBuffer = data.first()
            val length = min(srcBuffer.remaining(), minimumLeft)
            val savedLimit = srcBuffer.limit()
            buffer.put(srcBuffer.limit(srcBuffer.position() + length))
            srcBuffer.limit(savedLimit)
            reads += length
            minimumLeft -= length
            if (!srcBuffer.hasRemaining()) data.removeFirst()
            offset += length
        }
    }

    fun readStructure(type: KType, linker: Linker): Any = lock.read {
        return image(
            type,
            MemorySegment.ofArray(
                readBytes(type.computeLayout(linker).byteSize().toInt())
            ),
            true
        )
    }

    inline fun <reified T : Structure> readStructure(linker: Linker): T = readStructure(typeOf<T>(), linker) as T

    fun skip(length: Int) {
        var length = length
        suspend(length)
        while (length > 0) {
            val buffer = data.first()
            val skip = min(buffer.remaining(), length)
            buffer.position(buffer.position() + skip)
            reads += skip
            if (!buffer.hasRemaining()) data.removeFirst()
            length -= length
        }
    }

    fun align(to: Long) {
        val remainder = reads % to
        if (remainder == 0L) return
        skip((to - remainder).toInt())
    }

    fun load(data: ByteArray) = lock.write {
        this.data.addLast(ByteBuffer.wrap(data).order(ByteOrder.nativeOrder()))
        rCondition.signalAll()
    }
}