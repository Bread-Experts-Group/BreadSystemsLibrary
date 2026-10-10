package test

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.get
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.KernelBase
import org.bread_experts_group.bread_systems_library.ffm.windows.WinUSB
import org.bread_experts_group.bread_systems_library.ffm.windows.WindowsCleaningArena
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleLastError
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.fileapi.GENERIC_READ
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.fileapi.GENERIC_WRITE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.fileapi.OPEN_EXISTING
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winbase.FILE_FLAG_OVERLAPPED
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winusb.WINUSB_INTERFACE_HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec.USB_INTERFACE_DESCRIPTOR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec.WINUSB_PIPE_INFORMATION
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winusb.WINUSB_SETUP_PACKET
import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import kotlin.reflect.typeOf

fun main() {
    val linker = Linker.nativeLinker()
    val k32 = get<Kernel32>(SymbolLookup.libraryLookup("Kernel32.dll", Arena.ofAuto()), linker)
    val kBase = get<KernelBase>(SymbolLookup.libraryLookup("KernelBase.dll", Arena.ofAuto()), linker)
    val usb = get<WinUSB>(SymbolLookup.libraryLookup("Winusb.dll", Arena.ofAuto()), linker)

    val arena = WindowsCleaningArena(k32, kBase)
    val device = Pointer<WINUSB_INTERFACE_HANDLE>(arena)

    val handle = k32.CreateFileW!!(
        LPCWSTR(
            typeOf<WCHARptr>(),
            Arena.global().allocateFrom("\\\\?\\USB#VID_0BDA&PID_2838&MI_00#8&38a180a5&0&0000#{a819feb4-fd91-41de-befe-f34a3603b628}", Charsets.UTF_16LE)
        ), DWORD(GENERIC_READ or GENERIC_WRITE), DWORD(0),
        null, DWORD(OPEN_EXISTING), DWORD(FILE_FLAG_OVERLAPPED),
        null
    )
    if (handle.segment == INVALID_HANDLE_VALUE) handleLastError(k32)

    var status = usb.WinUsb_Initialize!!(handle, device)
    if (status.value == 0) handleLastError(k32)

    val descriptor = Pointer<USB_INTERFACE_DESCRIPTOR>(arena)
    status = usb.WinUsb_QueryInterfaceSettings!!(device.value, UCHAR(0.toByte()), descriptor)
    if (status.value == 0) handleLastError(k32)

    println(descriptor.value.bAlternateSetting)
    println(descriptor.value.bDescriptorType)
    println(descriptor.value.bInterfaceNumber)
    println(descriptor.value.bInterfaceProtocol)
    println(descriptor.value.bInterfaceSubClass)
    println(descriptor.value.bLength)
    println(descriptor.value.bNumEndpoints)
    println(descriptor.value.iInterface)
    println(descriptor.value.bInterfaceClass)

    val pipeInfo = Pointer<WINUSB_PIPE_INFORMATION>(arena)
    status = usb.WinUsb_QueryPipe!!(device.value, UCHAR(0.toByte()), UCHAR(0.toByte()), pipeInfo)
    if (status.value == 0) handleLastError(k32)

    println(pipeInfo.value.PipeType)
    println(pipeInfo.value.PipeId)
    println(pipeInfo.value.Interval)
    println(pipeInfo.value.MaximumPacketSize)

    val setup = Structure<WINUSB_SETUP_PACKET>(arena)
    setup.RequestType = UCHAR(0xC0.toByte())
    setup.Index = USHORT(0x0100.toShort())

    setup.Value = USHORT(0x2000.toShort())
    setup.Length = USHORT(1.toShort())

    val buffer = arena.allocate(1)
    val ptr = Pointer<ULONG>(typeOf<ULONG>(), arena.allocate(4))
    // Value = (BaseAdd)<<8 + OffsetAdd
    // Length = register length
    status = usb.WinUsb_ControlTransfer!!(
        device.value, setup,
        Pointer(typeOf<UCHAR>(), buffer), ULONG(1), ptr,
        null
    )
    if (status.value == 0) handleLastError(k32)
    println(buffer.toArray(ValueLayout.JAVA_BYTE).toHexString())
}