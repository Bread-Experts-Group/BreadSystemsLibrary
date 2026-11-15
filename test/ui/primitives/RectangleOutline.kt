package ui.primitives

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.float
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1Brush
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_RECT_F
import ui.RenderableElement
import ui.Scene
import ui.data.Rectangle
import kotlin.time.Duration

class RectangleOutline(
    private val rect: Rectangle<Float>,
    private val brush: Pointer<out ID2D1Brush>,
    private val thickness: Float
) : RenderableElement {
    override fun render(scene: Scene, deltaTime: Duration) {
        scene.useResources {
            val rect = configure<D2D1_RECT_F> {
                left = float(rect.leftTop.x)
                right = float(rect.rightBottom.x)
                top = float(rect.leftTop.y)
                bottom = float(rect.rightBottom.y)
            }
            scene.renderTarget.DrawRectangle(
                rect,
                brush,
                float(thickness)
            )
        }
    }
}