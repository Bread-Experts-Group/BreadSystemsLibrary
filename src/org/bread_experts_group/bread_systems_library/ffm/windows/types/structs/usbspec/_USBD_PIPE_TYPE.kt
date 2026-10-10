package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.usbspec

import org.bread_experts_group.bread_systems_library.ffm.c.types.int

typealias _USBD_PIPE_TYPE = int
typealias USBD_PIPE_TYPE = _USBD_PIPE_TYPE

const val UsbdPipeTypeControl = 0
const val UsbdPipeTypeIsochronous = 1
const val UsbdPipeTypeBulk = 2
const val UsbdPipeTypeInterrupt = 3