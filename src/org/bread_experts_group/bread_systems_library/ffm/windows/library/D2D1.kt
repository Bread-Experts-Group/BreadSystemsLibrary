package org.bread_experts_group.bread_systems_library.ffm.windows.library

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.void
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef.REFIID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_FACTORY_OPTIONS
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_FACTORY_TYPE
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused")
class D2D1(
    override val linker: Linker,
    val D2D1CreateFactory: ((factoryType: D2D1_FACTORY_TYPE, riid: REFIID?, pFactoryOptions: Pointer<D2D1_FACTORY_OPTIONS>?, ppIFactory: Pointer<void>?) -> HRESULT)?,
) : LibrarySource