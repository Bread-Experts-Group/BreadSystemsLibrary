package org.bread_experts_group.bread_systems_library.model.matrix.status.error

import org.bread_experts_group.bread_systems_library.model.codings.JSONName

data class MatrixError(
    @JSONName("errcode") val type: String,
    @JSONName("error") val message: String
) {
    override fun toString(): String = "MatrixError(type=\"$type\", message=\"$message\")"
}