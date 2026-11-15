package org.bread_experts_group.bread_systems_library.model.matrix.authentication

import org.bread_experts_group.bread_systems_library.model.JSONPrimitive

abstract class MatrixLoginAuthentication(type: String) : MatrixAuthentication(type) {
    abstract fun addToLogin(obj: MutableMap<String, JSONPrimitive?>)
}