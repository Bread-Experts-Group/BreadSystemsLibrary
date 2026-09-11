package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs

import org.bread_experts_group.bread_systems_library.ffm.TypeDynamicallySolved
import org.bread_experts_group.bread_systems_library.ffm.c.types.`unsigned __int64`
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import kotlin.reflect.KClass

class ULONGLONG(value: Any?) : ValueProvider(value) {
    companion object : TypeDynamicallySolved {
        override fun solve(): KClass<*> = when (System.getProperty("os.arch")) {
            "x86", "i386", "i486", "i586", "i686" -> TODO("Double")
            else -> `unsigned __int64`::class
        }
    }
}