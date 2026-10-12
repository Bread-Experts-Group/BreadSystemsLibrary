package org.bread_experts_group.bread_systems_library.generic

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.util.layout.computeLayout
import org.bread_experts_group.bread_systems_library.ffm.util.layout.image
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.math.min
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class FlushingOutput(private val buffer: MemorySegment, private val flush: (MemorySegment, Long) -> Unit) {
    var offset = 0L
        private set

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