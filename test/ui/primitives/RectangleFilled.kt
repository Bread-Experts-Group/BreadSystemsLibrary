package ui.primitives

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.float
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1Brush
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_RECT_F
import ui.RenderableElement
import ui.Scene
import ui.data.DynamicValue
import ui.data.Rectangle
import kotlin.time.Duration

class RectangleFilled(
    private val rect: Rectangle<DynamicValue<Float>>,
    private val brush: Pointer<out ID2D1Brush>
) : RenderableElement {
    override fun render(scene: Scene, deltaTime: Duration) {
        scene.useResources {
            val rect = configure<D2D1_RECT_F> {
                left = float(rect.leftTop.x.value)
                right = float(rect.rightBottom.x.value)
                top = float(rect.leftTop.y.value)
                bottom = float(rect.rightBottom.y.value)
            }
            scene.renderTarget.FillRectangle(
                rect,
                brush
            )
        }
    }
}