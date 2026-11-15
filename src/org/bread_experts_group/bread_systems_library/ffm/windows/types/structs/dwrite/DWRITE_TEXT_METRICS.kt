@file:Suppress("unused", "ClassName", "SpellCheckingInspection")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dwrite

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

interface DWRITE_TEXT_METRICS : Structure {
    @Order(0) var left: FLOAT
    @Order(1) var top: FLOAT
    @Order(2) var width: FLOAT
    @Order(3) var widthIncludingTrailingWhitespace: FLOAT
    @Order(4) var height: FLOAT
    @Order(5) var layoutWidth: FLOAT
    @Order(6) var layoutHeight: FLOAT
    @Order(7) var maxBidiReorderingDepth: UINT32
    @Order(8) var lineCount: UINT32
}