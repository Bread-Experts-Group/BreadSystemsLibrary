package org.bread_experts_group.bread_systems_library.model.matrix.status

import org.bread_experts_group.bread_systems_library.model.matrix.status.data.MatrixData
import org.bread_experts_group.bread_systems_library.model.matrix.status.error.MatrixError

class MatrixResponse<T : MatrixData> private constructor(private val type: T?, private val error: MatrixError?) {
    companion object {
        fun <T : MatrixData> ok(type: T) = MatrixResponse(type, null)
        fun <T : MatrixData> error(error: MatrixError) = MatrixResponse<T>(null, error)
    }

    val isOK: Boolean = type != null
    val isError: Boolean = error != null

    fun getOK(): T = type ?: throw IllegalArgumentException("Value not present: $error")
    fun getOKOrNull(): T? = type

    fun getError(): MatrixError = error ?: throw IllegalArgumentException("Error not present: $type")
    fun getErrorOrNull(): MatrixError? = error

    override fun toString(): String = if (isOK) "OK[$type]" else "Error[$error]"
}