package org.bread_experts_group.bread_systems_library.ffm.windows

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.PVOID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.SIZE_T
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ULONG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winnt.MEM_EXTENDED_PARAMETER
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused", "LocalVariableName")
class KernelBase(
    override val linker: Linker,
    val VirtualAlloc2: @PreserveLastError ((Process: HANDLE?, BaseAddress: PVOID?, Size: SIZE_T, AllocationType: ULONG, PageProtection: ULONG, ExtendedParameters: Pointer<MEM_EXTENDED_PARAMETER>?, ParameterCount: ULONG) -> PVOID)?,
) : LibrarySource