package org.bread_experts_group.bread_systems_library.ffm.windows.models.ioring

import java.lang.foreign.MemorySegment

class IORingTask : Throwable() {
    var information: Long = 0
        internal set(value) {
            if (complete) throw IllegalStateException("Already complete")
            field = value
        }
    var exception: Throwable? = null
        internal set(value) {
            if (complete) throw IllegalStateException("Already complete")
            field = value
        }
    var complete: Boolean = false
        internal set(value) {
            if (field) throw IllegalStateException("Already complete")
            field = value
        }
    internal var pinningSegment: MemorySegment? = null


    override fun toString(): String =
        if (complete) "Complete IO Ring Task($information${if (exception != null) ", $exception" else ""})"
        else "Incomplete IO Ring Task"
}