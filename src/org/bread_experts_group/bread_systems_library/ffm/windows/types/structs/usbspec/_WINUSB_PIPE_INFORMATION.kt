package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.USHORT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface _WINUSB_PIPE_INFORMATION : Structure {
    @Order(0) var PipeType: USBD_PIPE_TYPE
    @Order(1) var PipeId: UCHAR
    @Order(2) var MaximumPacketSize: USHORT
    @Order(3) var Interval: UCHAR
}

typealias WINUSB_PIPE_INFORMATION = _WINUSB_PIPE_INFORMATION
typealias PWINUSB_PIPE_INFORMATION = Pointer<_WINUSB_PIPE_INFORMATION>