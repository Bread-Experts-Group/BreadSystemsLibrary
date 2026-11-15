package org.bread_experts_group.bread_systems_library.model.matrix

import org.bread_experts_group.bread_systems_library.model.*
import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.codings.jsonDecode
import org.bread_experts_group.bread_systems_library.model.codings.jsonEncode
import org.bread_experts_group.bread_systems_library.model.matrix.authentication.MatrixAuthentication
import org.bread_experts_group.bread_systems_library.model.matrix.authentication.MatrixLoginAuthentication
import org.bread_experts_group.bread_systems_library.model.matrix.identifier.MatrixIdentifier
import org.bread_experts_group.bread_systems_library.model.matrix.status.MatrixResponse
import org.bread_experts_group.bread_systems_library.model.matrix.status.data.*
import org.bread_experts_group.bread_systems_library.model.matrix.status.error.MatrixError
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.SecureRandom
import kotlin.reflect.full.createType

class MatrixClient(
    private val client: HttpClient = HttpClient.newHttpClient(),
    private val baseServer: URI
) {
    private val secureRandom = SecureRandom()

    private val matrixClientV3Login = baseServer.resolve("/_matrix/client/v3/login")
    private val matrixClientV3Register = baseServer.resolve("/_matrix/client/v3/register")
    private val matrixClientV3RegisterEmailRequestToken = baseServer.resolve("/_matrix/client/v3/register/email/requestToken")

    private val matrixClientV3Capabilities = baseServer.resolve("/_matrix/client/v3/capabilities")
    private val matrixClientV3Sync = baseServer.resolve("/_matrix/client/v3/sync")
    private val matrixClientV3CreateRoom = baseServer.resolve("/_matrix/client/v3/createRoom")

    enum class RegistrationKind {
        USER,
        GUEST
    }

    data class RegistrationInformation(
        val initialDeviceDisplayName: String? = null,
        val noLogin: Boolean = false,
        val deviceID: String? = null,
        val password: String? = null,
        val username: String? = null,
        val authentication: MatrixAuthentication? = null
    )

    fun register(
        info: RegistrationInformation,
        kind: RegistrationKind = RegistrationKind.USER
    ): MatrixResponse<MatrixRegistrationResult> {
        val response = client.send(
            HttpRequest.newBuilder(
                URI("$matrixClientV3Register?kind=${kind.name.lowercase()}")
            ).POST(
                HttpRequest.BodyPublishers.ofString(
                    JSONObject(
                        buildMap {
                            if (info.initialDeviceDisplayName != null) put("initial_device_display_name", JSONString(info.initialDeviceDisplayName))
                            if (info.password != null) put("password", JSONString(info.password))
                            if (info.deviceID != null) put("device_id", JSONString(info.deviceID))
                            if (info.username != null) put("username", JSONString(info.username))
                            if (info.noLogin) put("inhibit_login", JSONBoolean(true))
                            put("refresh_token", JSONBoolean(true))
                            if (info.authentication != null) put(
                                "auth",
                                jsonEncode(info.authentication::class.createType(), info.authentication)
                            )
                        }
                    ).toString()
                )
            ).build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> return MatrixResponse.ok(jsonDecode<MatrixLegacyRegistration200>(jsonParse(data)))

            401 -> return MatrixResponse.ok(
                jsonDecode<MatrixLegacyRegistration401>(jsonParse(data))
            )

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }

    private val secretBuffer = ByteArray(64)
    fun registerEmail(email: String, nextLink: URI? = null, sendAttempt: BigInteger = BigInteger.ONE) {
        secureRandom.nextBytes(secretBuffer)
        val response = client.send(
            HttpRequest.newBuilder(
                matrixClientV3RegisterEmailRequestToken
            ).POST(
                HttpRequest.BodyPublishers.ofString(
                    JSONObject(
                        buildMap {
                            put("client_secret", JSONString(secretBuffer.toHexString()))
                            put("email", JSONString(email))
                            if (nextLink != null) put("next_link", JSONString(nextLink.toASCIIString()))
                            put("send_attempt", JSONNumber(BigDecimal(sendAttempt)))
                        }
                    ).toString()
                )
            ).build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        println(matrixClientV3RegisterEmailRequestToken)
        val data = response.body().bufferedReader(Charsets.UTF_8)
        println(data.readAllAsString())
        TODO("!")
    }

    data class LoginInformation(
        val initialDeviceDisplayName: String? = null,
        val deviceID: String? = null,
        val identifier: MatrixIdentifier,
        val authentication: MatrixLoginAuthentication
    )

    private var refreshToken: String? = null
    private var accessTokenExpiry: Long? = null
    private var accessTokenRaw: String? = null
    private val accessToken: String
        get() = accessTokenRaw ?: throw IllegalStateException("Access token not initialized yet")

    fun login(info: LoginInformation): MatrixResponse<MatrixLegacyLogin200> {
        val response = client.send(
            HttpRequest.newBuilder(matrixClientV3Login).POST(
                HttpRequest.BodyPublishers.ofString(
                    JSONObject(
                        buildMap {
                            if (info.initialDeviceDisplayName != null) put("initial_device_display_name", JSONString(info.initialDeviceDisplayName))
                            if (info.deviceID != null) put("device_id", JSONString(info.deviceID))
                            put("identifier", jsonEncode(info.identifier::class.createType(), info.identifier))
                            put("type", JSONString(info.authentication.type))
                            info.authentication.addToLogin(this)
                            put("refresh_token", JSONBoolean(true))
                        }
                    ).toString()
                )
            ).build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> {
                val login = jsonDecode<MatrixLegacyLogin200>(jsonParse(data))
                accessTokenExpiry = login.expiresInMs?.longValueExact()?.let { System.currentTimeMillis() + it }
                accessTokenRaw = login.accessToken
                refreshToken = login.refreshToken
                return MatrixResponse.ok(login)
            }

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }

    fun supportedLoginTypes(): MatrixResponse<MatrixLoginFlows> {
        val response = client.send(
            HttpRequest.newBuilder(matrixClientV3Login).build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> return MatrixResponse.ok(jsonDecode<MatrixLoginFlows>(jsonParse(data)))

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }

    fun capabilities(): MatrixResponse<MatrixCapabilities> {
        val response = client.send(
            HttpRequest.newBuilder(matrixClientV3Capabilities)
                .header("Authorization", "Bearer $accessToken").build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> return MatrixResponse.ok(jsonDecode<MatrixCapabilities>(jsonParse(data)))

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }

    fun synchronize(): MatrixResponse<MatrixSynchronization> {
        val response = client.send(
            HttpRequest.newBuilder(matrixClientV3Sync)
                .header("Authorization", "Bearer $accessToken").build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> return MatrixResponse.ok(
                jsonDecode<MatrixSynchronization>(jsonParse(data))
            )

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }

    data class CreateRoomInformation(
        @JSONName("creation_content") val creationContent: JSONObject? = null,
        @JSONName("initial_state") val initialState: List<StateEvent>? = null,
        val inviteUsers: List<MatrixID>? = null,
//      @JSONName("invite_3pid")  val invite3PIDs: List<Any>
        @JSONName("is_direct") val inviteDirect: Boolean = false,
        val name: String? = null,
//      @JSONName("power_level_content_override")  val powerLevelContentOverride: Any?,
        val preset: String? = null,
        @JSONName("room_alias_name") val aliasName: String? = null,
        @JSONName("room_version") val roomVersion: String? = null,
        val topic: String? = null,
        val visibility: String = "private"
    ) {
        data class StateEvent(
            val content: JSONObject,
            @JSONName("state_key") val stateKey: String = "",
            val type: String
        )
    }

    fun createRoom(info: CreateRoomInformation): MatrixResponse<MatrixCreateRoom200> {
        val response = client.send(
            HttpRequest.newBuilder(matrixClientV3CreateRoom).POST(
                HttpRequest.BodyPublishers.ofString(
                    jsonEncode<CreateRoomInformation>(info).toString()
                )
            ).header("Authorization", "Bearer $accessToken").build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = response.body().bufferedReader(Charsets.UTF_8)
        when (val status = response.statusCode()) {
            200 -> {
                val login = jsonDecode<MatrixCreateRoom200>(jsonParse(data))
                return MatrixResponse.ok(login)
            }

            in 400..499, in 500..599 -> {
                val error = jsonDecode<MatrixError>(jsonParse(data))
                if (status == 429 && error.type != "M_LIMIT_EXCEEDED") throw IllegalArgumentException(
                    "Compliance error: 429 response is not M_LIMIT_EXCEEDED."
                )
                return MatrixResponse.error(error)
            }

            else -> throw IllegalArgumentException(
                "Unknown status code received: $status"
            )
        }
    }
}