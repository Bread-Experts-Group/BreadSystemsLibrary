package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixID
import java.math.BigInteger

data class MatrixLegacyLogin200(
    @JSONName("access_token") val accessToken: String,
    @JSONName("device_id") val deviceID: String,
    @JSONName("expires_in_ms") val expiresInMs: BigInteger? = null,
    @JSONName("refresh_token") val refreshToken: String? = null,
    @JSONName("user_id") val userID: MatrixID,
    @JSONName("well_known") val discoveryInformation: DiscoveryInformation
) : MatrixData {
    data class DiscoveryInformation(
        @JSONName("m.homeserver") val homeserver: HomeServer,
        @JSONName("m.identity_server") val identityServer: IdentityServer? = null
    ) {
        data class HomeServer(@JSONName("base_url") val baseURL: String)
        data class IdentityServer(@JSONName("base_url") val baseURL: String)
    }
}