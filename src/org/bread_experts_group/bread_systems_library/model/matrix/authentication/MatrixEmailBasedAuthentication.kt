package org.bread_experts_group.bread_systems_library.model.matrix.authentication

import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.matrix.AUTH_TYPE_EMAIL_IDENTITY

data class MatrixEmailBasedAuthentication(
    @JSONName("threepid_creds") val threePidCredentials: ThreePIDCredentials,
    val session: String?
) : MatrixAuthentication(AUTH_TYPE_EMAIL_IDENTITY) {
    data class ThreePIDCredentials(
        val sid: String,
        @JSONName("client_secret") val clientSecret: String,
        @JSONName("id_server") val idServer: String?,
        @JSONName("id_access_token") val idAccessToken: String?
    )
}