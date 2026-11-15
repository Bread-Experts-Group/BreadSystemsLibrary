package ui

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.memoryLayout
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1HwndRenderTarget
import org.bread_experts_group.bread_systems_library.ffm.windows.models.window.WindowsWindow
import java.lang.foreign.Linker
import java.lang.foreign.SegmentAllocator
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.Duration

class Scene(
    val arena: SegmentAllocator,
    val window: WindowsWindow,
    val renderTarget: ID2D1HwndRenderTarget,
    val preDrawCommands: MutableList<Scene.(Duration) -> Unit> = mutableListOf(),
    val layers: MutableList<RenderableElement> = mutableListOf()
) {
    private val resources = mutableMapOf<KType, MutableList<Any>>()
    inner class ResourceReusing internal constructor() {
        private val positions = mutableMapOf<KType, Int>()
        fun configure(type: KType, configuration: Any.() -> Unit): Pointer<Any> {
            val place = positions[type] ?: 0
            positions[type] = place + 1
            val resourceList = resources.getOrPut(type) { mutableListOf() }
            val resource = resourceList.getOrNull(place)
            if (resource != null) {
                @Suppress("UNCHECKED_CAST")
                resource as Pointer<Any>
                configuration(resource.value)
                return resource
            }

            val ptr = Pointer<Any>(
                Linker.nativeLinker(), type,
                arena.allocate(type.memoryLayout(Linker.nativeLinker()))
            )
            resourceList.add(ptr)
            configuration(ptr.value)
            return ptr
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : Structure> configure(noinline configuration: T.() -> Unit): Pointer<T> = configure(
            typeOf<T>(),
            configuration as Any.() -> Unit
        ) as Pointer<T>
    }

    fun <R> useResources(configuration: ResourceReusing.() -> R): R = configuration(ResourceReusing())

    fun render(deltaTime: Duration) {
        renderTarget.BeginDraw()
        for (command in preDrawCommands) command(this, deltaTime)
        for (layer in layers) layer.render(this, deltaTime)
        renderTarget.EndDraw()
    }
}