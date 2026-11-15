package org.bread_experts_group.bread_systems_library.model.http.server

import org.bread_experts_group.bread_systems_library.model.http.server.request.RequestHandler

class ServerHosts {
    class Paths(
        val routes: MutableMap<Routable.Route, Paths> = mutableMapOf(),
        val terminal: MutableMap<String, RequestHandler> = mutableMapOf()
    ) {
        fun add(path: Routable, handler: RequestHandler, method: String) {
            var route = this
            when (path) {
                is Routable.Path -> {
                    path.paths.forEach { pathRoute ->
                        route = route.routes.getOrPut(pathRoute) { Paths() }
                    }
                }

                is Routable.Route -> route = route.routes.getOrPut(path) { Paths() }
            }
            if (route.terminal.containsKey(method)) throw IllegalArgumentException("$method already defined in $route")
            route.terminal[method] = handler
        }

        fun resolve(path: HostRoute): Pair<Map<String, RequestHandler>, List<String>>? {
            var route = this
            val arguments = mutableListOf<String>()
            when (path.route) {
                is Routable.Path -> {
                    path.route.paths.forEachIndexed { _, pathRoute ->
                        route = route.routes[pathRoute] ?: route.routes.entries.firstOrNull { it.key == Routable.Route.Argument }?.let {
                            arguments.add((pathRoute as Routable.Route.Segment).path)
                            it.value
                        } ?: return null
                    }
                }

                is Routable.Route -> route = route.routes[path.route] ?: return null
            }

            if (route.terminal.isEmpty()) return null
            return route.terminal to arguments
        }
    }

    val entries: Map<String?, Paths>
        field = mutableMapOf<String?, Paths>()

    operator fun get(host: String?): Paths? = entries[host]

    fun get(path: HostRoute, get: RequestHandler, head: RequestHandler = get): ServerHosts = this.also {
        val routes = entries.getOrPut(path.authority) { Paths() }
        routes.add(path.route, get, "GET")
        routes.add(path.route, head, "HEAD")
    }
}