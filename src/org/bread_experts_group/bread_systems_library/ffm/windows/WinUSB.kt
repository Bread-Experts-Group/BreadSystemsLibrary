package org.bread_experts_group.bread_systems_library.ffm.windows

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.*
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winusb.PWINUSB_INTERFACE_HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.winusb.WINUSB_INTERFACE_HANDLE
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase.LPOVERLAPPED
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec.PUSB_INTERFACE_DESCRIPTOR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec.PWINUSB_PIPE_INFORMATION
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winusb.WINUSB_SETUP_PACKET
import java.lang.foreign.Linker

@Suppress("PropertyName", "unused", "LocalVariableName")
class WinUSB(
    override val linker: Linker,
    val WinUsb_Initialize: @PreserveLastError ((DeviceHandle: HANDLE?, InterfaceHandle: PWINUSB_INTERFACE_HANDLE?) -> BOOL)?,
    val WinUsb_QueryInterfaceSettings: @PreserveLastError ((InterfaceHandle: WINUSB_INTERFACE_HANDLE?, AlternateInterfaceNumber: UCHAR, UsbAltInterfaceDescriptor: PUSB_INTERFACE_DESCRIPTOR?) -> BOOL)?,
    val WinUsb_QueryPipe: @PreserveLastError ((InterfaceHandle: WINUSB_INTERFACE_HANDLE?, AlternateInterfaceNumber: UCHAR, PipeIndex: UCHAR, PipeInformation: PWINUSB_PIPE_INFORMATION?) -> BOOL)?,
    val WinUsb_ControlTransfer: @PreserveLastError ((InterfaceHandle: WINUSB_INTERFACE_HANDLE?, SetupPacket: WINUSB_SETUP_PACKET, Buffer: PUCHAR?, BufferLength: ULONG, LengthTransferred: PULONG?, Overlapped: LPOVERLAPPED?) -> BOOL)?,
    val WinUsb_QueryDeviceInformation: @PreserveLastError ((InterfaceHandle: WINUSB_INTERFACE_HANDLE?, InformationType: LONG, BufferLength: PULONG?, buffer: PVOID?) -> BOOL)?,
) : LibrarySource