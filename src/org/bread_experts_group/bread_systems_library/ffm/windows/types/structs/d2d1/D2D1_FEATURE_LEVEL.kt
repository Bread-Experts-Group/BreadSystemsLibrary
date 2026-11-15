@file:Suppress("unused", "HasPlatformType")

package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d2d1

import org.bread_experts_group.bread_systems_library.ffm.c.types.int
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d3dcommon.D3D_FEATURE_LEVEL_10_0
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.d3dcommon.D3D_FEATURE_LEVEL_9_1

typealias D2D1_FEATURE_LEVEL = int

const val D2D1_FEATURE_LEVEL_DEFAULT = 0
const val D2D1_FEATURE_LEVEL_9 = D3D_FEATURE_LEVEL_9_1
const val D2D1_FEATURE_LEVEL_10 = D3D_FEATURE_LEVEL_10_0