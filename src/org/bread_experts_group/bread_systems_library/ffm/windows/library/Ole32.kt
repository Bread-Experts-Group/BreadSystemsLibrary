package org.bread_experts_group.bread_systems_library.ffm.windows.library

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef.GUID
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused")
class Ole32(
    override val linker: Linker,
    val CoCreateGuid: ((pguid: Pointer<GUID>?) -> HRESULT)?,
) : LibrarySource