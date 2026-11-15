package org.bread_experts_group.bread_systems_library.model.matrix.authentication

import org.bread_experts_group.bread_systems_library.model.matrix.AUTH_TYPE_REGISTRATION_TOKEN

data class MatrixTokenBasedAuthentication(
    val token: String,
    val session: String?
) : MatrixAuthentication(AUTH_TYPE_REGISTRATION_TOKEN)