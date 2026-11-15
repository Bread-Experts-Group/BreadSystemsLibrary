package ui.primitives

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1Brush
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_POINT_2F
import ui.RenderableElement
import ui.Scene
import ui.data.Point
import ui.data.TextLayout
import kotlin.time.Duration

class RenderedTextLayout(
    val origin: Point.D2<Float>,
    val layout: TextLayout,
    val fillBrush: Pointer<out ID2D1Brush>
) : RenderableElement {
    override fun render(scene: Scene, deltaTime: Duration) {
        scene.useResources {
            val origin = configure<D2D1_POINT_2F> {
                x = FLOAT(origin.x)
                y = FLOAT(origin.y)
            }.value
            scene.renderTarget.DrawTextLayout(
                origin,
                layout.layoutPtr,
                fillBrush
            )
        }
    }
}