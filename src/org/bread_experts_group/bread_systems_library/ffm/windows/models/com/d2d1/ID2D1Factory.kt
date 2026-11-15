package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_HWND_RENDER_TARGET_PROPERTIES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_RENDER_TARGET_PROPERTIES
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("unused", "FunctionName")
abstract class ID2D1Factory : IUnknown() {
    @Order(11)
    abstract fun CreateHwndRenderTarget(
        renderTargetProperties: Pointer<D2D1_RENDER_TARGET_PROPERTIES>?,
        hwndRenderTargetProperties: Pointer<D2D1_HWND_RENDER_TARGET_PROPERTIES>?,
        hwndRenderTarget: Pointer<in ID2D1HwndRenderTarget>?
    ): HRESULT

    companion object {
        const val GUID = "06152247-6f50-465a-9245-118bfd3b6007"
    }
}