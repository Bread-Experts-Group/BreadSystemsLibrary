package org.bread_experts_group.bread_systems_library.model.matrix.authentication

import org.bread_experts_group.bread_systems_library.model.JSONPrimitive
import org.bread_experts_group.bread_systems_library.model.JSONString
import org.bread_experts_group.bread_systems_library.model.matrix.AUTH_TYPE_PASSWORD
import org.bread_experts_group.bread_systems_library.model.matrix.identifier.MatrixIdentifier

data class MatrixPasswordBasedAuthentication(
    val password: String,
    val identifier: MatrixIdentifier? = null,
    val session: String? = null
) : MatrixLoginAuthentication(AUTH_TYPE_PASSWORD) {
    override fun addToLogin(obj: MutableMap<String, JSONPrimitive?>) {
        obj["password"] = JSONString(password)
    }
}