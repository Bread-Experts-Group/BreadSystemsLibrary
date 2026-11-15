package org.bread_experts_group.bread_systems_library.model.matrix.identifier

import org.bread_experts_group.bread_systems_library.model.matrix.IDENTIFIER_TYPE_USER

data class MatrixUser(
    val user: String
) : MatrixIdentifier(IDENTIFIER_TYPE_USER)