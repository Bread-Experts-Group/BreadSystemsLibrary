package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.codings.JSONName

data class MatrixCapabilities(
    val capabilities: MatrixCapabilitiesInner
) : MatrixData {
    data class BooleanCapability(val enabled: Boolean)
    data class AccountModerationCapability(
        val lock: Boolean = false,
        val suspend: Boolean = false
    )
    data class ProfileFieldsCapability(
        val allowed: List<String>? = null,
        val disallowed: List<String>? = null,
        val enabled: Boolean
    )
    data class RoomVersionsCapability(
        val available: Map<String, String>,
        val default: String
    )

    data class MatrixCapabilitiesInner(
        @JSONName("m.3pid_changes") val m3PIDAssociationChanges: BooleanCapability? = null,
        @JSONName("m.account_moderation") val accountModeration: AccountModerationCapability? = null,
        @JSONName("m.change_password") val changePassword: BooleanCapability? = null,
        @JSONName("m.forget_forced_upon_leave") val forgetRoomsOnceLeft: BooleanCapability? = null,
        @JSONName("m.get_login_token") val getLoginToken: BooleanCapability? = null,
        @JSONName("m.profile_fields") val extendedProfileFields: ProfileFieldsCapability? = null,
        @JSONName("m.room_versions") val roomVersions: RoomVersionsCapability? = null
    )
}