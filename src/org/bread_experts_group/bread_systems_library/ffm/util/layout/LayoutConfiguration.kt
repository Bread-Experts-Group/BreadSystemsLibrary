package org.bread_experts_group.bread_systems_library.ffm.util.layout

import java.lang.foreign.Linker
import java.lang.reflect.Type

data class LayoutConfiguration(
    val linker: Linker,
    val unaligned: Boolean,
    val type: Type
)
