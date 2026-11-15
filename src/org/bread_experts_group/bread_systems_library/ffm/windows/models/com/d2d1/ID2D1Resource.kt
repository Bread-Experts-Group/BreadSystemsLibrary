package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("FunctionName")
abstract class ID2D1Resource : IUnknown() {
    @Order(0)
    abstract fun GetFactory(
        factory : Pointer<in ID2D1Factory>?
    )
}