package org.bread_experts_group.bread_systems_library.ffm.util.layout

import java.lang.foreign.MemorySegment

sealed interface ImageMethodKind {
    class Getter(
        val handler: (MemorySegment) -> Any
    ) : ImageMethodKind

    class Setter(
        val handler: (MemorySegment, Any) -> Unit
    ) : ImageMethodKind

    class Struct(
        val image: (MemorySegment) -> Any
    ) : ImageMethodKind
}