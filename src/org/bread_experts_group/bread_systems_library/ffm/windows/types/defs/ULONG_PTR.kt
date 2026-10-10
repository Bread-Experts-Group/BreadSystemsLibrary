package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs

import org.bread_experts_group.bread_systems_library.ffm.TypeDynamicallySolved
import org.bread_experts_group.bread_systems_library.ffm.c.types.unsigned__int64
import org.bread_experts_group.bread_systems_library.ffm.c.types.unsigned_long
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("ClassName")
class ULONG_PTR(value: Any?) : ValueProvider(value) {
    companion object : TypeDynamicallySolved {
        override fun solve(): KType = when (System.getProperty("os.arch")) {
            "x86", "i386", "i486", "i586", "i686" -> typeOf<unsigned_long>()
            else -> typeOf<unsigned__int64>()
        }
    }
}