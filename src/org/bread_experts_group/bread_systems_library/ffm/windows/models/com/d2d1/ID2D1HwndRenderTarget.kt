@file:Suppress("FunctionName", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HWND
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_SIZE_U
import org.bread_experts_group.bread_systems_library.generic.Order

abstract class ID2D1HwndRenderTarget : ID2D1RenderTarget() {
    @Order(1)
    abstract fun Resize(pixelSize: Pointer<D2D1_SIZE_U>?): HRESULT

    @Order(2)
    abstract fun GetHwnd(): HWND
}