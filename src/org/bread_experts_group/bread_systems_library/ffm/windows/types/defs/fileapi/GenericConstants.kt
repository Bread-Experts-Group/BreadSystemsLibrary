package org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.fileapi

const val CREATE_NEW = 1
const val CREATE_ALWAYS = 2
const val OPEN_EXISTING = 3
const val OPEN_ALWAYS = 4
const val TRUNCATE_EXISTING = 5

const val FILE_SHARE_READ = 0x00000001
const val FILE_SHARE_WRITE = 0x00000002
const val FILE_SHARE_DELETE = 0x00000004

const val GENERIC_ALL = 0x10000000
const val GENERIC_EXECUTE = 0x20000000
const val GENERIC_WRITE = 0x40000000
const val GENERIC_READ = 0x80000000.toInt()