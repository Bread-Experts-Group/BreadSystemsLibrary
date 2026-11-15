package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixID
import java.math.BigInteger

data class MatrixLegacyRegistration200(
    @JSONName("access_token") val accessToken: String? = null,
    @JSONName("device_id") val deviceID: String? = null,
    @JSONName("expires_in_ms") val expiresInMs: BigInteger? = null,
    @JSONName("refresh_token") val refreshToken: String? = null,
    @JSONName("user_id") val userID: MatrixID
) : MatrixRegistrationResult