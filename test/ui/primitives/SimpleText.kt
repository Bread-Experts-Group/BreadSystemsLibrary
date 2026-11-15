package ui.primitives

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.float
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1Brush
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextFormat
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_RECT_F
import ui.RenderableElement
import ui.Scene
import ui.data.Rectangle
import kotlin.reflect.typeOf
import kotlin.time.Duration

class SimpleText(
    val text: String,
    val rect: Rectangle<Float>,
    val fontPtr: Pointer<IDWriteTextFormat>,
    val brushPtr: Pointer<out ID2D1Brush>,
    scene: Scene
) : RenderableElement {
    private val textPtr = Pointer<WCHAR>(
        scene.window.user32.linker,
        typeOf<WCHAR>(),
        scene.arena.allocateFrom(text, Charsets.UTF_16LE)
    )
    val textLength = UINT32(text.length)

    override fun render(scene: Scene, deltaTime: Duration) {
        scene.useResources {
            val rect = configure<D2D1_RECT_F> {
                left = float(rect.leftTop.x)
                right = float(rect.rightBottom.x)
                top = float(rect.leftTop.y)
                bottom = float(rect.rightBottom.y)
            }
            scene.renderTarget.DrawText(
                textPtr,
                textLength,
                fontPtr,
                rect,
                brushPtr
            )
        }
    }
}