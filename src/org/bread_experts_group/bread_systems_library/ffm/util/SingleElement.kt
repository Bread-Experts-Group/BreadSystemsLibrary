package org.bread_experts_group.bread_systems_library.ffm.util

import org.bread_experts_group.bread_systems_library.ffm.Structure

interface SingleElement<T> : Structure {
    var element: T
}