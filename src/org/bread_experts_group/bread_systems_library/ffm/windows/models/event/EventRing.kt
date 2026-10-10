package org.bread_experts_group.bread_systems_library.ffm.windows.models.event

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.util.SingleElement
import org.bread_experts_group.bread_systems_library.ffm.util.layout.NativeArray
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.KernelBase
import org.bread_experts_group.bread_systems_library.ffm.windows.WindowsCleaningArena
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleLastError
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.BOOL
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import java.lang.foreign.MemorySegment
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.reflect.typeOf

const val MAXIMUM_WAIT_OBJECTS = 64

object EventRing {
    private class EventSet(
        val arena: WindowsCleaningArena,
        val threadSets: MutableList<ThreadSet>
    )

    private class ThreadSet(
        val kernel32: Kernel32,
        val wakeupEvent: HANDLE,
        val events: ConcurrentHashMap<WeakReference<HANDLE>, () -> Unit>
    )

    private val threads = mutableMapOf<Kernel32, EventSet>()

    private val orphanRefMap = mutableMapOf<WeakReference<HANDLE>, Pair<ThreadSet, HANDLE>>()
    private val orphanEventsQueue = ReferenceQueue<HANDLE>()

    init {
        Thread.ofVirtual().name("Orphan Event Cleaner").start {
            val notifyingSets = mutableMapOf<ThreadSet, MutableList<HANDLE>>()

            while (true) {
                var weak = orphanEventsQueue.remove()
                while (weak != null) {
                    val (threadSet, handle) = orphanRefMap.remove(weak)!!
                    synchronized(threadSet) {
                        threadSet.events.remove(weak)
                        notifyingSets.getOrPut(threadSet) { mutableListOf() }.add(handle)
                    }
                    weak = orphanEventsQueue.poll()
                }
                notifyingSets.forEach { (set, handles) ->
                    if (set.kernel32.SetEvent!!(set.wakeupEvent).value == 0) handleLastError(set.kernel32)
                    handles.forEach { handle ->
                        if (set.kernel32.CloseHandle!!(handle).value == 0) handleLastError(set.kernel32)
                    }
                }
                notifyingSets.clear()
            }
        }
    }

    private fun getEventSet(kernel32: Kernel32, kernelBase: KernelBase) = threads.getOrPut(kernel32) {
        EventSet(
            WindowsCleaningArena(kernel32, kernelBase),
            mutableListOf()
        )
    }

    private var counter = 0L
    fun addEvent(kernel32: Kernel32, kernelBase: KernelBase, event: HANDLE, callback: () -> Unit) {
        val eventSet = getEventSet(kernel32, kernelBase)

        val handleCopy = Pointer<Void>(typeOf<Void>(), event.segment)
        val weak = WeakReference(event, orphanEventsQueue)

        if (eventSet.threadSets.isEmpty() || eventSet.threadSets.last().events.size == MAXIMUM_WAIT_OBJECTS - 1) {
            val wakeupEvent = kernel32.CreateEventW!!(null, BOOL(0), BOOL(0), null)
            if (wakeupEvent.segment == MemorySegment.NULL) handleLastError(kernel32)
            val localSet = ThreadSet(
                kernel32,
                wakeupEvent,
                ConcurrentHashMap<WeakReference<HANDLE>, () -> Unit>().also {
                    it[weak] = callback
                }
            )
            orphanRefMap[weak] = localSet to handleCopy
            eventSet.threadSets.add(localSet)
            thread(isDaemon = true, name = "Event Manager ${counter++}]") {
                val reverse = mutableListOf<Pair<WeakReference<HANDLE>, () -> Unit>>()
                var handles: MemorySegment = MemorySegment.NULL
                var count = 0

                fun allocateSet() {
                    reverse.clear()
                    val strong = localSet.events.mapNotNull {
                        val ref = it.key.get() ?: return@mapNotNull null
                        reverse.add(it.key to it.value)
                        ref to it.value
                    }
                    handles = NativeArray(eventSet.arena, strong.size + 1) { index, element: SingleElement<HANDLE> ->
                        if (index == 0) element.element = wakeupEvent
                        else element.element = strong[index - 1].first
                    }
                    count = strong.size + 1
                }

                allocateSet()
                while (true) {
                    val status = kernel32.WaitForMultipleObjectsEx!!(
                        DWORD(count), Pointer(typeOf<HANDLE>(), handles),
                        BOOL(0), DWORD(-1), BOOL(0)
                    ).value as Int
                    when (status) {
                        0 -> {
                            if (localSet.events.isEmpty()) {
                                if (kernel32.CloseHandle!!(wakeupEvent).value == 0) handleLastError(kernel32)
                                eventSet.threadSets.remove(localSet)
                                return@thread
                            }
                            allocateSet()
                        }

                        in 1..<MAXIMUM_WAIT_OBJECTS -> {
                            val (ref, callback) = reverse[status - 1]
                            val handle = ref.get()
                            if (handle == null) {
                                allocateSet()
                                continue
                            }
                            callback()
                        }

                        -1 -> handleLastError(kernel32)
                        else -> throw NotImplementedError("Status...$status")
                    }
                }
            }
        } else {
            val availableThreadSet = eventSet.threadSets.last()
            orphanRefMap[weak] = availableThreadSet to handleCopy
            availableThreadSet.events[weak] = callback
            if (kernel32.SetEvent!!(availableThreadSet.wakeupEvent).value == 0) handleLastError(kernel32)
        }
    }
}