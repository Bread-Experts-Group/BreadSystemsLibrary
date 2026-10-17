package org.bread_experts_group.bread_systems_library.generic

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class FlushingOutput(
    private val buffer: MemorySegment,
    private val flush: (segment: MemorySegment, size: Long) -> Unit
) {
    open var offset = 0L
        protected set

    class Buffer(byteBuffer: ByteBuffer, flush: (buffer: ByteBuffer, size: Int) -> Unit) : FlushingOutput(
        MemorySegment.ofBuffer(byteBuffer),
        { segment, size -> flush(segment.asByteBuffer(), size.toInt()) }
    )

    inner class WithEndian : FlushingOutput(buffer, flush) {
        override var offset: Long
            get() = super.offset
            set(value) {
                super.offset = value
            }

        var endian: ByteOrder = ByteOrder.nativeOrder()

        override fun writeChar(data: Char) {
            super.writeChar(if (endian != ByteOrder.nativeOrder()) Character.reverseBytes(data) else data)
        }

        override fun writeShort(data: Short) {
            super.writeShort(if (endian != ByteOrder.nativeOrder()) java.lang.Short.reverseBytes(data) else data)
        }

        override fun writeInt(data: Int) {
            super.writeInt(if (endian != ByteOrder.nativeOrder()) Integer.reverseBytes(data) else data)
        }

        override fun writeLong(data: Long) {
            super.writeLong(if (endian != ByteOrder.nativeOrder()) java.lang.Long.reverseBytes(data) else data)
        }

        override fun writeFloat(data: Float) {
            writeInt(java.lang.Float.floatToRawIntBits(data))
        }

        override fun writeDouble(data: Double) {
            writeLong(java.lang.Double.doubleToRawLongBits(data))
        }
    }

    fun writeByte(data: Byte) {
        if (Byte.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Byte.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_BYTE, offset] = data
        offset += Byte.SIZE_BYTES
    }

    open fun writeChar(data: Char) {
        if (Char.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Char.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_CHAR_UNALIGNED, offset] = data
        offset += Char.SIZE_BYTES
    }

    open fun writeShort(data: Short) {
        if (Short.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Short.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_SHORT_UNALIGNED, offset] = data
        offset += Short.SIZE_BYTES
    }

    open fun writeInt(data: Int) {
        if (Int.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Int.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_INT_UNALIGNED, offset] = data
        offset += Int.SIZE_BYTES
    }

    open fun writeLong(data: Long) {
        if (Long.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Long.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_LONG_UNALIGNED, offset] = data
        offset += Long.SIZE_BYTES
    }

    open fun writeFloat(data: Float) {
        if (Float.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Float.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_FLOAT_UNALIGNED, offset] = data
        offset += Float.SIZE_BYTES
    }

    open fun writeDouble(data: Double) {
        if (Double.SIZE_BYTES > buffer.byteSize()) TODO("Lrg part push")
        if (Double.SIZE_BYTES > (buffer.byteSize() - offset)) flush()
        buffer[ValueLayout.JAVA_DOUBLE_UNALIGNED, offset] = data
        offset += Double.SIZE_BYTES
    }

    fun writeData(data: ByteArray) {
        if (data.size > buffer.byteSize()) TODO("Lrg part push")
        if (data.size > (buffer.byteSize() - offset)) flush()
        MemorySegment.copy(
            data, 0,
            buffer, ValueLayout.JAVA_BYTE, offset,
            data.size
        )
        offset += data.size
    }

    fun writeStructure(linker: Linker, type: KType, run: Any.() -> Unit) {
        val size = type.computeLayout(linker).byteSize()
        if (size > buffer.byteSize()) TODO("Lrg part push")
        if (size > (buffer.byteSize() - offset)) flush()
        run(image(type, buffer.asSlice(offset)))
        offset += size
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Structure> writeStructure(linker: Linker, noinline run: T.() -> Unit) = writeStructure(
        linker, typeOf<T>(), run as (Any) -> Unit
    )

    fun skip(count: Long) {
        var count = count
        val bufferSize = buffer.byteSize()
        while (count > 0) {
            val skip = min(bufferSize - offset, count)
            offset += skip
            count -= skip
            if (offset == bufferSize) flush()
        }
    }

    fun align(to: Long) {
        val remainder = offset % to
        if (remainder == 0L) return
        skip(to - remainder)
    }

    fun flush() {
        this.flush(buffer, offset)
        offset = 0
    }
}