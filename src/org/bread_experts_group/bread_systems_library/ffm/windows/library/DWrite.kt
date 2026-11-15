package org.bread_experts_group.bread_systems_library.ffm.windows.library

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef.REFIID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_FACTORY_TYPE
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused")
class DWrite(
    override val linker: Linker,
    val DWriteCreateFactory: ((factoryType: DWRITE_FACTORY_TYPE, riid: REFIID?, factory: Pointer<out IUnknown>?) -> HRESULT)?,
) : LibrarySource