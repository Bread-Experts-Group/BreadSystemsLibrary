package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.c.types.int

typealias IORING_SQE_FLAGS = int

const val IOSQE_FLAGS_NONE = 0
const val IOSQE_FLAGS_DRAIN_PRECEDING_OPS = 1