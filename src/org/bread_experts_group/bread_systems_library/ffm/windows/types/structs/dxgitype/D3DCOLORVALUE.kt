@file:Suppress("SpellCheckingInspection", "unused")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dxgitype

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.c.types.float
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName")
interface _D3DCOLORVALUE : Structure {
    @Order(0) var r: float
    @Order(1) var g: float
    @Order(2) var b: float
    @Order(3) var a: float
}

typealias D3DCOLORVALUE = _D3DCOLORVALUE