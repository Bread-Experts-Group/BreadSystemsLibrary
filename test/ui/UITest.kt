package ui

import org.bread_experts_group.bread_systems_library.ffm.c.types.*
import org.bread_experts_group.bread_systems_library.ffm.get
import org.bread_experts_group.bread_systems_library.ffm.windows.WindowsCleaningArena
import org.bread_experts_group.bread_systems_library.ffm.windows.library.*
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1Factory
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1HwndRenderTarget
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.d2d1.ID2D1SolidColorBrush
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteFactory
import org.bread_experts_group.bread_systems_library.ffm.windows.models.com.dwrite.IDWriteTextFormat
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleResult
import org.bread_experts_group.bread_systems_library.ffm.windows.models.window.WindowsWindow
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_COLOR_F
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.d2d1.D2D1_SIZE_U
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.guiddef.GUID
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winuser.WM_SIZE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_FACTORY_TYPE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_FACTORY_TYPE_MULTI_THREADED
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_HWND_RENDER_TARGET_PROPERTIES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1.D2D1_RENDER_TARGET_PROPERTIES
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.windef.RECT
import ui.data.DynamicValue
import ui.data.Point
import ui.data.Rectangle
import ui.data.TextLayout
import ui.primitives.RectangleFilled
import ui.primitives.RenderedTextLayout
import java.lang.foreign.*
import java.util.*
import java.util.concurrent.locks.LockSupport
import kotlin.reflect.typeOf
import kotlin.system.measureNanoTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val linker = Linker.nativeLinker()
private val u32 = get<User32>(SymbolLookup.libraryLookup("User32.dll", Arena.ofAuto()), linker)
private val k32 = get<Kernel32>(SymbolLookup.libraryLookup("Kernel32.dll", Arena.ofAuto()), linker)
private val kBase = get<KernelBase>(SymbolLookup.libraryLookup("KernelBase.dll", Arena.ofAuto()), linker)
private val ole32 = get<Ole32>(SymbolLookup.libraryLookup("Ole32.dll", Arena.ofAuto()), linker)
private val d2d1 = get<D2D1>(SymbolLookup.libraryLookup("D2D1.dll", Arena.ofAuto()), linker)
private val dw = get<DWrite>(SymbolLookup.libraryLookup("Dwrite.dll", Arena.ofAuto()), linker)

fun main() {
    val cleaning = WindowsCleaningArena(k32, kBase)

    val guid = Pointer<GUID>(cleaning)

    fun GUID.guid(uuid: UUID) {
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits
        Data1 = unsigned_long((msb ushr 32).toInt())
        Data2 = unsigned_short(((msb ushr 16) and 0xFFFF).toShort())
        Data3 = unsigned_short((msb and 0xFFFF).toShort())
        Data4[0] = unsigned_char(((lsb ushr 56) and 0xFF).toByte())
        Data4[1] = unsigned_char(((lsb ushr 48) and 0xFF).toByte())
        Data4[2] = unsigned_char(((lsb ushr 40) and 0xFF).toByte())
        Data4[3] = unsigned_char(((lsb ushr 32) and 0xFF).toByte())
        Data4[4] = unsigned_char(((lsb ushr 24) and 0xFF).toByte())
        Data4[5] = unsigned_char(((lsb ushr 16) and 0xFF).toByte())
        Data4[6] = unsigned_char(((lsb ushr 8) and 0xFF).toByte())
        Data4[7] = unsigned_char((lsb and 0xFF).toByte())
    }

    guid.value.guid(UUID.fromString(ID2D1Factory.GUID))

    val window = WindowsWindow(k32, u32, cleaning)

    val d2d1Factory = comPointer<ID2D1Factory>(cleaning).also {
        @Suppress("UNCHECKED_CAST")
        val status = d2d1.D2D1CreateFactory!!(
            D2D1_FACTORY_TYPE(D2D1_FACTORY_TYPE_MULTI_THREADED),
            guid, null,
            it as Pointer<void>
        )
        if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(k32, status)
    }.value

    var szX = 0f
    var szY = 0f
    val renderTarget = comPointer<ID2D1HwndRenderTarget>(cleaning).also {
        val clientArea = Pointer<RECT>(cleaning)
        u32.GetClientRect!!(window.hWnd, clientArea)
        szX = (clientArea.value.right.value as Int).toFloat()
        szY = (clientArea.value.bottom.value as Int).toFloat()

        val status = d2d1Factory.CreateHwndRenderTarget(
            Pointer<D2D1_RENDER_TARGET_PROPERTIES>(cleaning),
            Pointer<D2D1_HWND_RENDER_TARGET_PROPERTIES>(cleaning).also { propPtr ->
                propPtr.value.apply {
                    this.hwnd = window.hWnd
                    this.pixelSize.width = UINT32(szX.toInt())
                    this.pixelSize.height = UINT32(szY.toInt())
                }
            },
            it
        )
        if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(k32, status)
    }.value

    guid.value.guid(UUID.fromString(IDWriteFactory.GUID))

    val dWriteFactoryPtr = comPointer<IDWriteFactory>(cleaning).also {
        val status = dw.DWriteCreateFactory!!(
            DWRITE_FACTORY_TYPE(DWRITE_FACTORY_TYPE_ISOLATED),
            guid,
            it
        )
        if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(k32, status)
    }
    val dWriteFactory = dWriteFactoryPtr.value

    val font = comPointer<IDWriteTextFormat>(cleaning).also {
        val status = dWriteFactory.CreateTextFormat(
            Pointer(
                linker,
                typeOf<WCHAR>(),
                cleaning.allocateFrom("Segoe UI", Charsets.UTF_16LE)
            ),
            null,
            DWRITE_FONT_WEIGHT(DWRITE_FONT_WEIGHT_NORMAL),
            DWRITE_FONT_STYLE(DWRITE_FONT_STYLE_NORMAL),
            DWRITE_FONT_STRETCH(DWRITE_FONT_STRETCH_NORMAL),
            FLOAT(15f),
            Pointer(
                linker,
                typeOf<WCHAR>(),
                cleaning.allocateFrom("en-US", Charsets.UTF_16LE)
            ),
            it
        )
        if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(k32, status)
    }.value
    val fontPtr = Pointer<IDWriteTextFormat>(font.segment)

    val d2SizePtr = Pointer<D2D1_SIZE_U>(cleaning)
    val d2Size = d2SizePtr.value

    val scene = Scene(cleaning, window, renderTarget)

    window.messageHandlers[UINT(WM_SIZE)] = { _, lParam ->
        val dimensions = lParam.value as Long

        val width = (dimensions and 0xFFFF).toInt()
        val height = ((dimensions shr 16) and 0xFFFF).toInt()

        szX = width.toFloat()
        szY = height.toFloat()
        d2Size.width = UINT32(width)
        d2Size.height = UINT32(height)
        renderTarget.Resize(d2SizePtr)
        LRESULT(0)
    }

    val targetFPS = 60L
    val frameDuration = 1_000_000_000L / targetFPS
    var lastFrame = System.nanoTime()

    scene.preDrawCommands.add {
        scene.useResources {
            renderTarget.Clear(
                configure<D2D1_COLOR_F> {
                    r = float(0.77f)
                    g = float(0.77f)
                    b = float(0.77f)
                    a = float(1f)
                }
            )
        }
    }

    class WindowSizeReactive<T>(val result: (x: Float, y: Float) -> T) : DynamicValue<T> {
        override val value: T
            get() = result(szX, szY)
    }

    fun createBrush() {
        val brush = comPointer<ID2D1SolidColorBrush>(cleaning).also {
            val status = renderTarget.CreateSolidColorBrush(
                Pointer<D2D1_COLOR_F>(cleaning).also { color ->
                    color.value.apply {
                        r = float(1f)
                        g = float(1f)
                        b = float(1f)
                        a = float(1f)
                    }
                },
                it
            )
            if (it.segment[ValueLayout.ADDRESS, 0] == MemorySegment.NULL) handleResult(k32, status)
        }.value
        val brushPtr = Pointer<ID2D1SolidColorBrush>(brush.segment)
    }

    val topPadding = 5f
    fun localWindow(name: String) {
        scene.layers.clear()
        val layout = TextLayout(name, fontPtr, dWriteFactory, scene)
        scene.layers.add(RenderedTextLayout(Point.D2(0f, topPadding), layout, brushPtr))
        scene.layers.addFirst(
            RectangleFilled(
                Rectangle(
                    Point.D2(DynamicValue.Fixed(0f), DynamicValue.Fixed(topPadding)),
                    Point.D2(
                        scene.useResources {
                            val clientArea = configure<RECT> {}
                            u32.GetClientRect!!(window.hWnd, clientArea)
                            WindowSizeReactive { x, _ -> x }
                        },
                        DynamicValue.Fixed((layout.getMetrics().height.value as Float) + topPadding)
                    )
                ),
                brushPtr
            )
        )
    }

    fun ncView() {
        localWindow("Network Client")
    }
    ncView()

    while (true) {
        val now = System.nanoTime()
        val deltaTime = now - lastFrame
        lastFrame = now
        val frameTime = measureNanoTime {
            scene.render(deltaTime.toDuration(DurationUnit.NANOSECONDS))
        }
        LockSupport.parkNanos(frameDuration - frameTime)
    }
}