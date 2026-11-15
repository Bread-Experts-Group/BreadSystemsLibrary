package org.bread_experts_group.bread_systems_library.ffm.windows.models.window

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.handle
import org.bread_experts_group.bread_systems_library.ffm.windows.library.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.library.User32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winuser.CW_USEDEFAULT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winuser.WS_OVERLAPPEDWINDOW
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winuser.WS_VISIBLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser.MSG
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winuser.WNDCLASSEXW
import java.lang.foreign.SegmentAllocator
import java.lang.invoke.MethodHandles
import kotlin.reflect.typeOf

class WindowsWindow(
    val kernel32: Kernel32,
    val user32: User32,
    arena: SegmentAllocator
) {
    val messageHandlers = mutableMapOf<UINT, WindowsWindow.(wParam: WPARAM, lParam: LPARAM) -> LRESULT>()
    lateinit var hWnd: HWND
        private set

    fun wndProc(hWnd: HWND, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        return messageHandlers[msg]?.invoke(this, wParam, lParam) ?:
        user32.DefWindowProcW!!(hWnd, msg, wParam, lParam)
    }

    init {
        val className = LPCWSTR(
            kernel32.linker,
            typeOf<WCHARptr>(),
            arena.allocateFrom("TestClass", Charsets.UTF_16LE)
        )
        val hInstance = kernel32.GetModuleHandleW!!(null)
        val lookup = MethodHandles.lookup()
        user32.RegisterClassExW!!(
            Pointer<WNDCLASSEXW>(arena).also {
                it.value.apply {
                    this.cbSize = UINT(it.segment.byteSize().toInt())
                    this.setUpcall(::lpfnWndProc, ::wndProc.handle(lookup).bindTo(this@WindowsWindow))
                    this.hInstance = hInstance
                    this.lpszClassName = className
                }
            }
        )

        Thread.ofPlatform().daemon().start {
            hWnd = user32.CreateWindowExW!!(
                DWORD(0),
                className, null, DWORD(WS_OVERLAPPEDWINDOW or WS_VISIBLE),
                int(CW_USEDEFAULT), int(CW_USEDEFAULT), int(CW_USEDEFAULT), int(CW_USEDEFAULT),
                null, null, hInstance, null
            )

            val msg = Pointer<MSG>(arena)
            while (user32.GetMessageW!!(msg, hWnd, UINT(0), UINT(0)).value as Int > 0) {
                user32.TranslateMessage!!(msg)
                user32.DispatchMessageW!!(msg)
            }
        }
    }
}