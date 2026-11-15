package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs

import org.bread_experts_group.bread_systems_library.ffm.TypeDynamicallySolved
import org.bread_experts_group.bread_systems_library.ffm.c.types.__int64
import org.bread_experts_group.bread_systems_library.ffm.c.types.long
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("ClassName")
class LONG_PTR(value: Any?) : ValueProvider(value) {
    companion object : TypeDynamicallySolved {
        override fun solve(): KType = when (System.getProperty("os.arch")) {
            "x86", "i386", "i486", "i586", "i686" -> typeOf<long>()
            else -> typeOf<__int64>()
        }
    }
}