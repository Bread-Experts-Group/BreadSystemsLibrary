package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UCHAR
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface _USB_INTERFACE_DESCRIPTOR : Structure {
    @Order(0) var bLength: UCHAR
    @Order(1) var bDescriptorType: UCHAR
    @Order(2) var bInterfaceNumber: UCHAR
    @Order(3) var bAlternateSetting: UCHAR
    @Order(4) var bNumEndpoints: UCHAR
    @Order(5) var bInterfaceClass: UCHAR
    @Order(6) var bInterfaceSubClass: UCHAR
    @Order(7) var bInterfaceProtocol: UCHAR
    @Order(8) var iInterface: UCHAR
}

typealias USB_INTERFACE_DESCRIPTOR = _USB_INTERFACE_DESCRIPTOR
typealias PUSB_INTERFACE_DESCRIPTOR = Pointer<_USB_INTERFACE_DESCRIPTOR>