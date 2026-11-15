package org.bread_experts_group.bread_systems_library.model.matrix

import org.bread_experts_group.bread_systems_library.model.codings.JSONName

data class MatrixWellKnownClientProperties(
    @JSONName("m.homeserver") val mHomeserver: HomeServer
) {
    data class HomeServer(
        @JSONName("base_url") val baseURL: String
    )
}