package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.codings.JSONName

data class MatrixCreateRoom200(
    @JSONName("room_id") val roomID: MatrixSynchronization.RoomID
) : MatrixRegistrationResult