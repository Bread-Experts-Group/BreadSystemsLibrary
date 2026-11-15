package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.JSONObject
import org.bread_experts_group.bread_systems_library.model.JSONString
import org.bread_experts_group.bread_systems_library.model.codings.IJSONDeserializer
import org.bread_experts_group.bread_systems_library.model.codings.JSONDeserializer
import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.codings.jsonDecode

data class MatrixLoginFlows(
    val flows: List<LoginFlow>
) : MatrixData {
    @JSONDeserializer(LoginFlow.Serializer::class)
    sealed class LoginFlow(
        open val type: String
    ) {
        object Serializer : IJSONDeserializer<JSONObject, LoginFlow> {
            override fun jsonDeserialize(input: JSONObject?): LoginFlow {
                val type = (input!!.elements["type"] as JSONString).value
                if (type == "m.login.token") return jsonDecode<LoginToken>(input)
                return jsonDecode<Generic>(input)
            }
        }

        data class Generic(
            override val type: String
        ) : LoginFlow(type)

        data class LoginToken(
            @JSONName("get_login_token") val getLoginToken: Boolean?
        ) : LoginFlow("m.login.token")
    }
}