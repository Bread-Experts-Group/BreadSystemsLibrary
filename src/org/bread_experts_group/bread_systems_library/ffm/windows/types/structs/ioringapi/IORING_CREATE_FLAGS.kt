@file:Suppress("PropertyName")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface IORING_CREATE_FLAGS : Structure {
    @Order(0) var Required: IORING_CREATE_REQUIRED_FLAGS
    @Order(1) var Advisory: IORING_CREATE_ADVISORY_FLAGS
}