package org.bread_experts_group.bread_systems_library.model.http.server

import java.net.URLEncoder

sealed interface Routable {
    value class Path(val paths: List<Route>) : Routable {
        constructor(vararg paths: Route) : this(paths.toList())
    }

    sealed interface Route : Routable {
        class Segment(path: String) : Route {
            val path: String = URLEncoder.encode(path, Charsets.UTF_8)
        }

        object Argument : Route
    }
}