package org.bread_experts_group.bread_systems_library.ffm.windows.models.ioring

import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.KernelBase
import org.bread_experts_group.bread_systems_library.ffm.windows.models.event.EventRing
import org.bread_experts_group.bread_systems_library.ffm.windows.models.exception.HRESULTException
import org.bread_experts_group.bread_systems_library.ffm.windows.models.exception.status.FACILITY_NT_IORING
import org.bread_experts_group.bread_systems_library.ffm.windows.models.exception.status.IORING_COMPLETION_QUEUE_TOO_FULL
import org.bread_experts_group.bread_systems_library.ffm.windows.models.exception.status.IORING_SUBMISSION_QUEUE_FULL
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleLastError
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.BOOL
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import java.lang.foreign.MemorySegment
import java.lang.ref.Cleaner
import java.util.*
import java.util.concurrent.CompletableFuture

class ThreadedIORing(private val kernel32: Kernel32, kernelBase: KernelBase) : AutoCloseable {
    companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    private val ioRing = IORing(kernel32, kernelBase)!!
    private val taskMap = IdentityHashMap<IORingTask, CompletableFuture<Long>>()

    private val event = kernel32.CreateEventW!!(null, BOOL(0), BOOL(0), null).also {
        if (it.segment == MemorySegment.NULL) handleLastError(kernel32)
        ioRing.associateEvent(it)

        val taskMap = taskMap
        val ioRing = ioRing

        EventRing.addEvent(kernel32, kernelBase, it) {
            synchronized(taskMap) {
                while (true) {
                    val task = ioRing.popCompletion() ?: break
                    task.exception.let { exception ->
                        val localTask = taskMap.remove(task)!!
                        if (exception != null) localTask.completeExceptionally(exception)
                        else localTask.complete(task.information)
                    }
                }
            }
        }
    }

    private val cleanable = cleaner.register(this, object : Runnable {
        val ioRing = this@ThreadedIORing.ioRing

        override fun run() {
            ioRing.close()
            // EventRing manages lifetime of event
        }
    })

    override fun close() {
        cleanable.clean()
    }

    fun register(handles: Array<out HANDLE>) {
        try {
            synchronized(taskMap) {
                val task = ioRing.register(handles)
                taskMap[task] = CompletableFuture<Long>()
            }
        } catch (e: HRESULTException) {
            if (e.facility == FACILITY_NT_IORING && e.code == IORING_SUBMISSION_QUEUE_FULL) {
                submit()
                return register(handles)
            } else throw e
        }
    }

    fun register(segments: Array<MemorySegment>) {
        try {
            synchronized(taskMap) {
                val task = ioRing.register(segments)
                taskMap[task] = CompletableFuture<Long>()
            }
        } catch (e: HRESULTException) {
            if (e.facility == FACILITY_NT_IORING && e.code == IORING_SUBMISSION_QUEUE_FULL) {
                submit()
                return register(segments)
            } else throw e
        }
    }

    fun fillHandleRef(handle: HANDLE) = ioRing.fillHandleRef(handle)
    fun fillBufferRef(buffer: MemorySegment, offset: Int) = ioRing.fillBufferRef(buffer, offset)

    fun submit() {
        try {
            ioRing.submit()
        } catch (e: HRESULTException) {
            if (e.facility == FACILITY_NT_IORING && e.code == IORING_COMPLETION_QUEUE_TOO_FULL) {
                if (kernel32.SetEvent!!(event).value as Int == 0) handleLastError(kernel32)
                return submit()
            } else throw e
        }
    }

    fun readAsync(
        handleOffset: Long, readLength: Int
    ): CompletableFuture<Long> {
        try {
            synchronized(taskMap) {
                val task = ioRing.queueRead(handleOffset, readLength)
                val future = CompletableFuture<Long>()
                taskMap[task] = future
                return future
            }
        } catch (e: HRESULTException) {
            if (e.facility == FACILITY_NT_IORING && e.code == IORING_SUBMISSION_QUEUE_FULL) {
                submit()
                return readAsync(handleOffset, readLength)
            } else throw e
        }
    }

    fun readAsync(
        handle: HANDLE, handleOffset: Long, readLength: Int
    ): CompletableFuture<Long> {
        ioRing.fillHandleRef(handle)
        return readAsync(handleOffset, readLength)
    }

    fun readAsync(
        handleOffset: Long, readLength: Int,
        buffer: MemorySegment, bufferOffset: Int
    ): CompletableFuture<Long> {
        ioRing.fillBufferRef(buffer, bufferOffset)
        return readAsync(handleOffset, readLength)
    }

    fun readAsync(
        handle: HANDLE, handleOffset: Long, readLength: Int,
        buffer: MemorySegment, bufferOffset: Int
    ): CompletableFuture<Long> {
        ioRing.fillHandleRef(handle)
        ioRing.fillBufferRef(buffer, bufferOffset)
        return readAsync(handleOffset, readLength)
    }

    fun writeAsync(
        handleOffset: Long, readLength: Int
    ): CompletableFuture<Long> {
        try {
            synchronized(taskMap) {
                val task = ioRing.queueWrite(handleOffset, readLength)
                val future = CompletableFuture<Long>()
                taskMap[task] = future
                return future
            }
        } catch (e: HRESULTException) {
            if (e.facility == FACILITY_NT_IORING && e.code == IORING_SUBMISSION_QUEUE_FULL) {
                submit()
                return writeAsync(handleOffset, readLength)
            } else throw e
        }
    }

    fun writeAsync(
        handle: HANDLE, handleOffset: Long, readLength: Int
    ): CompletableFuture<Long> {
        ioRing.fillHandleRef(handle)
        return writeAsync(handleOffset, readLength)
    }

    fun writeAsync(
        handleOffset: Long, readLength: Int,
        buffer: MemorySegment, bufferOffset: Int
    ): CompletableFuture<Long> {
        ioRing.fillBufferRef(buffer, bufferOffset)
        return writeAsync(handleOffset, readLength)
    }

    fun writeAsync(
        handle: HANDLE, handleOffset: Long, readLength: Int,
        buffer: MemorySegment, bufferOffset: Int
    ): CompletableFuture<Long> {
        ioRing.fillHandleRef(handle)
        ioRing.fillBufferRef(buffer, bufferOffset)
        return writeAsync(handleOffset, readLength)
    }
}