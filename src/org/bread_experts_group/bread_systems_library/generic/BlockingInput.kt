package org.bread_experts_group.bread_systems_library.generic

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    var reads = 0L

    private fun hasEnough(space: Int): Boolean {
        if (space == 0) return true
        if (data.isEmpty()) return false
        var count = 0
        for (slice in data) {
            count += slice.remaining()
            if (count >= space) return true
        }
        return false
    }

    private fun suspend(space: Int) {
        while (!hasEnough(space)) lock.write {
            rCondition.await()
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

    fun readInt(): Int = lock.read {
        suspend(Int.SIZE_BYTES)
        val available = data.first()
        val value = available.getInt()
        reads += Int.SIZE_BYTES
        if (!available.hasRemaining()) data.removeFirst()
        return value
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