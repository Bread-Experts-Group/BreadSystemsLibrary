package org.bread_experts_group.bread_systems_library.ffm.util

import org.bread_experts_group.bread_systems_library.ffm.Structure

interface ArrayElement<T> : Structure {
    operator fun get(index: Long): T
    operator fun set(index: Long, value: T)
}