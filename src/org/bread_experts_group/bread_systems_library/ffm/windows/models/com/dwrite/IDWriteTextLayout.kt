@file:Suppress("FunctionName", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_TEXT_METRICS
import org.bread_experts_group.bread_systems_library.generic.Order

abstract class IDWriteTextLayout : IDWriteTextFormat() {
    @Order(32)
    abstract fun GetMetrics(textMetrics: Pointer<DWRITE_TEXT_METRICS>?): HRESULT
}