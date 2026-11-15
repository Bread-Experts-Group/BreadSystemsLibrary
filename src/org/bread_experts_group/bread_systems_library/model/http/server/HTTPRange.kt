package org.bread_experts_group.bread_systems_library.model.http.server

sealed class HTTPRange {
    abstract val start: Long?
    abstract val end: Long?
    data class Suffix(override val end: Long) : HTTPRange() {
        override val start: Long? = null
    }

    data class ToEnd(override val start: Long) : HTTPRange() {
        override val end: Long? = null
    }

    data class Range(override val start: Long, override val end: Long) : HTTPRange()
}