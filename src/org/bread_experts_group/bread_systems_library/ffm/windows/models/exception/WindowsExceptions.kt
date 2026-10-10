package org.bread_experts_group.bread_systems_library.ffm.windows.models.exception

class HRESULTException(
    val severe: Boolean,
    val facility: Int,
    val code: Int,
    message: String
) : RuntimeException(
    "(${facility.toUInt().toHexString().padStart(6, '0')}) ${code.toUInt().toHexString().padStart(8, '0')}: $message"
)

class CustomerException(
    val code: Int
) : RuntimeException()

class NTSTATUSException(
    val severity: Severity,
    val facility: Int,
    val code: Int,
    message: String
) : RuntimeException(
    "(${facility.toUInt().toHexString().padStart(6, '0')}) ${code.toUInt().toHexString().padStart(8, '0')}: $severity: $message"
) {
    enum class Severity {
        SUCCESS,
        INFORMATION,
        WARNING,
        ERROR
    }
}

fun hResult(code: Int, message: String): RuntimeException {
    if (code ushr 29 and 1 == 1) return CustomerException(code)
    return HRESULTException(
        code ushr 31 == 1,
        code ushr 16 and ((1 shl 11) - 1),
        code and ((1 shl 16) - 1),
        message
    )
}

fun ntStatus(code: Int, message: String): RuntimeException {
    if (code ushr 29 and 1 == 1) return CustomerException(code)
    return NTSTATUSException(
        NTSTATUSException.Severity.entries[code ushr 30],
        code ushr 16 and ((1 shl 12) - 1),
        code and ((1 shl 16) - 1),
        message
    )
}