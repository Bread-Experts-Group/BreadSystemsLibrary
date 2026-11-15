package ui.data

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.comPointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteFactory
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextFormat
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextLayout
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleResult
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.DWRITE_TEXT_METRICS
import ui.Scene
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.reflect.typeOf

class TextLayout(
    val string: String,
    format: Pointer<IDWriteTextFormat>,
    factory: IDWriteFactory,
    private val scene: Scene,
    maxWidth: Float = Float.MAX_VALUE, maxHeight: Float = Float.MAX_VALUE
) {
    private val layout: IDWriteTextLayout = comPointer<IDWriteTextLayout>(scene.arena).also {
        val status = factory.CreateTextLayout(
            Pointer(
                scene.window.user32.linker,
                typeOf<WCHAR>(),
                scene.arena.allocateFrom(string, Charsets.UTF_16LE)
            ),
            UINT32(string.length),
            format,
            FLOAT(maxWidth), FLOAT(maxHeight),
            it
        )
        if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(scene.window.kernel32, status)
    }.value
    internal val layoutPtr = Pointer<IDWriteTextLayout>(layout.segment)

    fun getMetrics(): DWRITE_TEXT_METRICS = scene.useResources {
        val metrics = configure<DWRITE_TEXT_METRICS> {}
        layout.GetMetrics(metrics)
        metrics.value
    }
}