@file:Suppress("unused", "ClassName", "PropertyName", "SpellCheckingInspection")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.dcommon

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.util.ArrayElement
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.FLOAT
import org.bread_experts_group.bread_systems_library.generic.ArraySize
import org.bread_experts_group.bread_systems_library.generic.Order

interface D2D_MATRIX_3X2_F : Structure {
    @Order(0) var DUMMYUNIONNAME: _DUMMYUNIONNAME

    interface _DUMMYUNIONNAME : Union {
        @Order(2) val DUMMYSTRUCTNAME: _DUMMYSTRUCTNAME
        @Order(2) val DUMMYSTRUCTNAME2: _DUMMYSTRUCTNAME2
        @Order(3) val m: @ArraySize(3) ArrayElement<@ArraySize(2) ArrayElement<FLOAT>>

        interface _DUMMYSTRUCTNAME : Structure {
            @Order(0) var m11: FLOAT
            @Order(1) var m12: FLOAT
            @Order(2) var m21: FLOAT
            @Order(3) var m22: FLOAT
            @Order(4) var dx: FLOAT
            @Order(5) var dy: FLOAT
        }

        interface _DUMMYSTRUCTNAME2 : Structure {
            @Order(0) var _11: FLOAT
            @Order(1) var _12: FLOAT
            @Order(2) var _21: FLOAT
            @Order(3) var _22: FLOAT
            @Order(4) var _31: FLOAT
            @Order(5) var _32: FLOAT
        }
    }
}