package org.bread_experts_group.bread_systems_library.ffm.linux

const val NETLINK_GENERIC = 16

const val NLM_F_REQUEST = 0x01
const val NLM_F_MULTI = 0x02
const val NLM_F_ACK = 0x04
const val NLM_F_ECHO = 0x08
const val NLM_F_DUMP_INTR = 0x10
const val NLM_F_DUMP_FILTERED = 0x20

const val NLM_F_ROOT = 0X100
const val NLM_F_MATCH = 0x200
const val NLM_F_ATOMIC = 0x400
const val NLM_F_DUMP = NLM_F_ROOT or NLM_F_MATCH

const val NLMSG_MIN_TYPE = 0x10

const val NLMSG_NOOP = 0x1
const val NLMSG_ERROR = 0x2
const val NLMSG_DONE = 0x3
const val NLMSG_OVERRUN = 0x4