package org.bread_experts_group.bread_systems_library.ffm.windows.models.ioring

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.util.SingleElement
import org.bread_experts_group.bread_systems_library.ffm.util.layout.Array
import org.bread_experts_group.bread_systems_library.ffm.util.layout.NativeArray
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.KernelBase
import org.bread_experts_group.bread_systems_library.ffm.windows.WindowsCleaningArena
import org.bread_experts_group.bread_systems_library.ffm.windows.models.getResult
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleResult
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ioringapi.HIORING
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ntioring_x.IORING_BUFFER_INFO
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FILE_WRITE_FLAGS
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winbase.FILE_WRITE_FLAGS_NONE
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.ref.Cleaner
import kotlin.reflect.typeOf

class IORing internal constructor(
    private val kernel32: Kernel32,
    kernelBase: KernelBase,
    private val hIo: HIORING
): AutoCloseable {
    private companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    private val arena = WindowsCleaningArena(kernel32, kernelBase)

    private val buffersSegment = mutableMapOf<MemorySegment, Int>()
    private val handlesSegment = mutableMapOf<HANDLE, Int>()

    private var taskCounter = Long.MAX_VALUE
    private val tasks = mutableMapOf<Long, IORingTask>()
    private fun addTask() = IORingTask().also {
        tasks[++taskCounter] = it
    }

    private val cleanable = cleaner.register(this, object : Runnable {
        private val k32 = this@IORing.kernel32
        private val hio = this@IORing.hIo

        override fun run() {
            handleResult(k32, k32.CloseIoRing!!(hio))
        }
    })

    private var active = true
    override fun close() {
        active = false
        cleanable.clean()
    }

    fun allocate(lengths: IntArray): Pair<IORingTask, Array<MemorySegment>> {
        if (!active) throw IORingClosed()
        buffersSegment.clear()

        val (segments, array) = Array(arena, lengths.size) { index, bufInfo: IORING_BUFFER_INFO ->
            val length = lengths[index]
            val segment = arena.allocate(length.toLong())
            bufInfo.Address = Pointer(segment)
            bufInfo.Length = UINT32(length)
            buffersSegment[segment] = index
            segment
        }

        val task = addTask()
        handleResult(
            kernel32,
            kernel32.BuildIoRingRegisterBuffers!!(
                hIo,
                UINT32(lengths.size), Pointer(array),
                UINT_PTR(taskCounter)
            )
        )

        return task to segments
    }

    fun register(segments: Array<MemorySegment>): IORingTask {
        if (!active) throw IORingClosed()
        buffersSegment.clear()

        val array = NativeArray(arena, segments.size) { index, bufInfo: IORING_BUFFER_INFO ->
            val segment = segments[index]
            bufInfo.Address = Pointer(segment)
            bufInfo.Length = UINT32(segment.byteSize().toInt())
            buffersSegment[segment] = index
        }

        val task = addTask()
        task.pinningSegment = array
        handleResult(
            kernel32,
            kernel32.BuildIoRingRegisterBuffers!!(
                hIo,
                UINT32(segments.size), Pointer(array),
                UINT_PTR(taskCounter)
            )
        )

        return task
    }

    fun register(handles: Array<out HANDLE>): IORingTask {
        if (!active) throw IORingClosed()
        handlesSegment.clear()

        val array = NativeArray(arena, handles.size) { index, handle: SingleElement<HANDLE> ->
            val fileHandle = handles[index]
            handle.element = fileHandle
            handlesSegment[fileHandle] = index
        }

        val task = addTask()
        task.pinningSegment = array
        handleResult(
            kernel32,
            kernel32.BuildIoRingRegisterFileHandles!!(
                hIo,
                UINT32(handles.size), Pointer(typeOf<Pointer<Nothing>>(), array),
                UINT_PTR(taskCounter)
            )
        )
        return task
    }

    private val handleRef = ThreadLocal.withInitial {
        Structure<IORING_HANDLE_REF>(arena)
    }
    fun fillHandleRef(handle: HANDLE): Unit = handleRef.get().let {
        val handleIndex = handlesSegment[handle]
        if (handleIndex != null) {
            it.Kind = IORING_REF_KIND(IORING_REF_REGISTERED)
            it.Handle.Index = UINT32(handleIndex)
        } else {
            it.Kind = IORING_REF_KIND(IORING_REF_RAW)
            it.Handle.Handle = handle
        }
    }

    private val bufferRef = ThreadLocal.withInitial {
        Structure<IORING_BUFFER_REF>(arena)
    }
    fun fillBufferRef(buffer: MemorySegment, offset: Int): Unit = bufferRef.get().let {
        val bufferIndex = buffersSegment[buffer]
        if (bufferIndex != null) {
            it.Kind = IORING_REF_KIND(IORING_REF_REGISTERED)
            it.Buffer.IndexAndOffset.BufferIndex = UINT32(bufferIndex)
            it.Buffer.IndexAndOffset.Offset = UINT32(offset)
        } else {
            it.Kind = IORING_REF_KIND(IORING_REF_RAW)
            it.Buffer.Address = Pointer(typeOf<Void>(), buffer.asSlice(offset.toLong()))
        }
    }

    fun queueRead(fileOffset: Long, length: Int): IORingTask {
        if (!active) throw IORingClosed()

        val task = addTask()
        handleResult(
            kernel32,
            kernel32.BuildIoRingReadFile!!(
                hIo,
                handleRef.get(), bufferRef.get(),
                UINT32(length), UINT64(fileOffset),
                UINT_PTR(taskCounter), IORING_SQE_FLAGS(0)
            )
        )
        return task
    }

    fun queueWrite(fileOffset: Long, length: Int): IORingTask {
        if (!active) throw IORingClosed()

        val task = addTask()
        handleResult(
            kernel32,
            kernel32.BuildIoRingWriteFile!!(
                hIo,
                handleRef.get(), bufferRef.get(),
                UINT32(length),
                UINT64(fileOffset),
                FILE_WRITE_FLAGS(FILE_WRITE_FLAGS_NONE),
                UINT_PTR(taskCounter),
                IORING_SQE_FLAGS(0)
            )
        )
        return task
    }

    fun submit() {
        if (!active) throw IORingClosed()
        handleResult(
            kernel32,
            kernel32.SubmitIoRing!!(hIo, UINT32(0), UINT32(0), null)
        )
    }

    private val cqe = ThreadLocal.withInitial {
        Pointer<IORING_CQE>(arena)
    }
    fun popCompletion(): IORingTask? {
        if (!active) throw IORingClosed()
        val cqe = cqe.get()
        val result = kernel32.PopIoRingCompletion!!(hIo, cqe)
        if (result.value as Int == 1) return null
        handleResult(kernel32, result)
        val task = tasks.remove(cqe.value.UserData.value as Long)!!
        task.exception = getResult(kernel32, DWORD(cqe.value.ResultCode.value), task)
        task.information = cqe.value.Information.value as Long
        task.complete = true
        task.pinningSegment = null
        return task
    }

    fun associateEvent(event: HANDLE) {
        handleResult(
            kernel32,
            kernel32.SetIoRingCompletionEvent!!(hIo, event)
        )
    }
}

fun IORing(
    kernel32: Kernel32,
    kernelBase: KernelBase
): IORing? {
    kernel32.QueryIoRingCapabilities ?: return null
    kernel32.CreateIoRing ?: return null

    val hIo = Arena.ofConfined().use {
        val capabilities = Pointer<IORING_CAPABILITIES>(it)
        if (
            (capabilities.value.FeatureFlags.value as Number).toInt() and IORING_FEATURE_UM_EMULATION != 0
        ) return null

        handleResult(
            kernel32,
            kernel32.QueryIoRingCapabilities(capabilities)
        )
        val flags = Structure<IORING_CREATE_FLAGS>(it)
        val hIo = Pointer<HIORING>(it)
        handleResult(
            kernel32,
            kernel32.CreateIoRing(
                capabilities.value.MaxVersion, flags,
                capabilities.value.MaxSubmissionQueueSize, capabilities.value.MaxCompletionQueueSize,
                hIo
            )
        )
        hIo.value
    }

    return IORing(kernel32, kernelBase, hIo)
}