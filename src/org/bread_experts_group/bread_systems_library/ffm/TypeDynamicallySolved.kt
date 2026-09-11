package org.bread_experts_group.bread_systems_library.ffm

import kotlin.reflect.KClass

interface TypeDynamicallySolved {
    fun solve(): KClass<*>
}