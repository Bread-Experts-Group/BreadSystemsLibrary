package org.bread_experts_group.bread_systems_library.model.http.server.request

import org.bread_experts_group.bread_systems_library.model.http.server.HostRoute
import org.bread_experts_group.bread_systems_library.model.http.server.response.ResponseWriter

abstract class RequestHandler {
    abstract fun handle(
        path: HostRoute,
        arguments: List<String>,
        output: ResponseWriter
    )

    /**
     * @return `true`: no changes have occurred, transmit status 304 (not modified). `false`: standard handling
     */
    fun notModified(
        path: HostRoute,
        arguments: Array<String>,
        range: List<LongRange>?
    ): Boolean = false
}