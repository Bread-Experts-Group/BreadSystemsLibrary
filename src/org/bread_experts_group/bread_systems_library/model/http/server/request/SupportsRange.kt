package org.bread_experts_group.bread_systems_library.model.http.server.request

import org.bread_experts_group.bread_systems_library.model.http.server.HTTPRange
import org.bread_experts_group.bread_systems_library.model.http.server.HostRoute
import org.bread_experts_group.bread_systems_library.model.http.server.response.ResponseWriter

interface SupportsRange {
    fun handleHeaders(
        path: HostRoute,
        arguments: List<String>,
        output: ResponseWriter
    ): Boolean

    fun handleRange(
        path: HostRoute,
        arguments: List<String>,
        range: HTTPRange,
        data: (ByteArray) -> Unit
    )

    /**
     * @return `true`: range is satisfiable. `false`: range is unsatisfiable, transmit status 416 (range not satisfiable).
     */
    fun rangeSatisfiable(
        path: HostRoute,
        arguments: List<String>,
        range: List<HTTPRange>
    ): Boolean
}