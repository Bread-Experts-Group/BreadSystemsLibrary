package org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextFormat
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextLayout
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_COLOR_F
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_POINT_2F
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_RECT_F
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_TAG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_BRUSH_PROPERTIES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_DRAW_TEXT_OPTIONS
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_DRAW_TEXT_OPTIONS_NONE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_RENDER_TARGET_PROPERTIES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon.DWRITE_MEASURING_MODE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon.DWRITE_MEASURING_MODE_NATURAL
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("unused", "FunctionName")
abstract class ID2D1RenderTarget : ID2D1Resource() {
    @Order(4)
    abstract fun CreateSolidColorBrush(
        color: Pointer<D2D1_COLOR_F>?,
        brushProperties: Pointer<D2D1_BRUSH_PROPERTIES>?,
        solidColorBrush: Pointer<in ID2D1SolidColorBrush>?
    ): HRESULT

    fun CreateSolidColorBrush(
        color: Pointer<D2D1_COLOR_F>?,
        solidColorBrush: Pointer<in ID2D1SolidColorBrush>?
    ): HRESULT = CreateSolidColorBrush(color, null, solidColorBrush)

    @Order(11)
    abstract fun DrawLine(
        point0: D2D1_POINT_2F,
        point1: D2D1_POINT_2F,
        brush: Pointer<ID2D1Brush>?,
        strokeWidth: FLOAT = FLOAT(1.0f),
        strokeStyle: Pointer<ID2D1StrokeStyle>? = null
    )

    @Order(12)
    abstract fun DrawRectangle(
        rect: Pointer<D2D1_RECT_F>?,
        brush: Pointer<out ID2D1Brush>?,
        strokeWidth: FLOAT = FLOAT(1.0f),
        strokeStyle: Pointer<ID2D1StrokeStyle>? = null
    )

    @Order(13)
    abstract fun FillRectangle(
        rect: Pointer<D2D1_RECT_F>?,
        brush: Pointer<out ID2D1Brush>?
    )

    @Order(23)
    abstract fun DrawText(
        string: Pointer<WCHAR>?,
        stringLength: UINT32,
        textFormat: Pointer<IDWriteTextFormat>?,
        layoutRect: Pointer<D2D1_RECT_F>?,
        defaultFillBrush: Pointer<out ID2D1Brush>?,
        options: D2D1_DRAW_TEXT_OPTIONS = int(D2D1_DRAW_TEXT_OPTIONS_NONE),
        measuringMode: DWRITE_MEASURING_MODE = int(DWRITE_MEASURING_MODE_NATURAL)
    )

    @Order(24)
    abstract fun DrawTextLayout(
        origin: D2D1_POINT_2F,
        textLayout: Pointer<IDWriteTextLayout>?,
        defaultFillBrush: Pointer<out ID2D1Brush>?,
        options: D2D1_DRAW_TEXT_OPTIONS = int(D2D1_DRAW_TEXT_OPTIONS_NONE)
    )

    @Order(43)
    abstract fun Clear(clearColor: Pointer<D2D1_COLOR_F>? = null)

    @Order(44)
    abstract fun BeginDraw()

    @Order(45)
    abstract fun EndDraw(tag1: Pointer<D2D1_TAG>? = null, tag2: Pointer<D2D1_TAG>? = null): HRESULT

    @Order(52)
    abstract fun IsSupported(renderTargetProperties: Pointer<D2D1_RENDER_TARGET_PROPERTIES>?): BOOL
}