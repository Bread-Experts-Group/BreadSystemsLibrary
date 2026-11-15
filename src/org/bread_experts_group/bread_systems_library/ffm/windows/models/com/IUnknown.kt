package org.bread_experts_group.bread_systems_library.ffm.windows.models.com

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.void
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ULONG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef.REFIID
import org.bread_experts_group.bread_systems_library.generic.Order
import java.lang.foreign.MemorySegment

@Suppress("unused", "FunctionName")
abstract class IUnknown {
    abstract val segment: MemorySegment

    @Order(0) abstract fun QueryInterface(riid: REFIID, ppvObject: Pointer<void>?): HRESULT
    @Order(1) abstract fun AddRef(): ULONG
    @Order(2) abstract fun Release(): ULONG

    companion object {
        const val GUID = "00000000-0000-0000-C000-000000000046"
    }
}