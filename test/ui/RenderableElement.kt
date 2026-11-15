package ui

import kotlin.time.Duration

interface RenderableElement {
    fun render(scene: Scene, deltaTime: Duration)
}