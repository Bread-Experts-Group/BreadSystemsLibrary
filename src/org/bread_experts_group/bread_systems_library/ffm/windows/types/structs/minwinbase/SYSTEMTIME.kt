@file:Suppress("unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WORD
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface _SYSTEMTIME : Structure {
    @Order(0) var wYear: WORD
    @Order(1) var wMonth: WORD
    @Order(2) var wDayOfWeek: WORD
    @Order(3) var wDay: WORD
    @Order(4) var wHour: WORD
    @Order(5) var wMinute: WORD
    @Order(6) var wSecond: WORD
    @Order(7) var wMilliseconds: WORD
}

typealias SYSTEMTIME = _SYSTEMTIME
typealias PSYSTEMTIME = Pointer<_SYSTEMTIME>
typealias LPSYSTEMTIME = Pointer<_SYSTEMTIME>