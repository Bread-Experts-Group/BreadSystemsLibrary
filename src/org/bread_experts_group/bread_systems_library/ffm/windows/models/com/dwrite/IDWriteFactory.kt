@file:Suppress("FunctionName")

package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.IUnknown
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.HRESULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_FONT_STRETCH
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_FONT_STYLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_FONT_WEIGHT
import org.bread_experts_group.bread_systems_library.generic.Order

abstract class IDWriteFactory : IUnknown() {
    @Order(12)
    abstract fun CreateTextFormat(
        fontFamilyName: Pointer<WCHAR>?,
        fontCollection: Pointer<IDWriteFontCollection>?,
        fontWeight: DWRITE_FONT_WEIGHT,
        fontStyle: DWRITE_FONT_STYLE,
        fontStretch: DWRITE_FONT_STRETCH,
        fontSize: FLOAT,
        localeName: Pointer<WCHAR>?,
        textFormat: Pointer<IDWriteTextFormat>?
    ): HRESULT

    @Order(15)
    abstract fun CreateTextLayout(
        string: Pointer<WCHAR>?,
        stringLength: UINT32,
        textFormat: Pointer<IDWriteTextFormat>?,
        maxWidth: FLOAT,
        maxHeight: FLOAT,
        textLayout: Pointer<IDWriteTextLayout>?
    ): HRESULT

    companion object {
        const val GUID = "b859ee5a-d838-4b5b-a2e8-1adc7d93db48"
    }
}