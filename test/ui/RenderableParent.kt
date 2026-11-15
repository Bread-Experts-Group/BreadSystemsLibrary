package ui

import kotlin.time.Duration

abstract class RenderableParent : RenderableElement {
    val children = mutableListOf<RenderableElement>()
    final override fun render(scene: Scene, deltaTime: Duration) {
        renderParent(scene, deltaTime)
        for (child in children) child.render(scene, deltaTime)
    }

    abstract fun renderParent(scene: Scene, deltaTime: Duration)
}