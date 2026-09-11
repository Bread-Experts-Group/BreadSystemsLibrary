package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.wchar_t

@Suppress("DANGEROUS_CHARACTERS")
typealias WCHARptr = Pointer<WCHAR>
class WCHAR(value: Any?) : wchar_t(value)