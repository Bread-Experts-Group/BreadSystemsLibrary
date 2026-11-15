package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.JSONPrimitive

data class MatrixLegacyRegistration401(
    val completed: List<String> = emptyList(),
    val flows: List<FlowInformation>,
    val params: Map<String, JSONPrimitive> = emptyMap(),
    val session: String?
) : MatrixRegistrationResult {
    data class FlowInformation(
        val stages: List<String>
    )
}