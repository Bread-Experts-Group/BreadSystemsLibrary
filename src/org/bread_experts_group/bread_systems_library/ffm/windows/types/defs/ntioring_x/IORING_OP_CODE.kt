package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.ntioring_x

import org.bread_experts_group.bread_systems_library.ffm.c.types.int

typealias IORING_OP_CODE = int

const val IORING_OP_NOP = 0
const val IORING_OP_READ = 1
const val IORING_OP_REGISTER_FILES = 2
const val IORING_OP_REGISTER_BUFFERS = 3
const val IORING_OP_CANCEL = 4
const val IORING_OP_WRITE = 5
const val IORING_OP_FLUSH = 6
const val IORING_OP_READ_SCATTER = 7
const val IORING_OP_WRITE_GATHER = 8