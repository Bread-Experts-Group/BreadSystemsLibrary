package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winusb

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UCHAR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.USHORT
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface _WINUSB_SETUP_PACKET : Structure {
    @Order(0) var RequestType: UCHAR
    @Order(1) var Request: UCHAR
    @Order(2) var Value: USHORT
    @Order(3) var Index: USHORT
    @Order(4) var Length: USHORT
}

typealias WINUSB_SETUP_PACKET = _WINUSB_SETUP_PACKET
typealias PWINUSB_SETUP_PACKET = Pointer<_WINUSB_SETUP_PACKET >