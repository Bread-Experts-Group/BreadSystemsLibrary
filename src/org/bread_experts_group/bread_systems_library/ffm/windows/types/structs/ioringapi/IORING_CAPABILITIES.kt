package org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.ioringapi

import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.UINT32
import org.bread_experts_group.bread_systems_library.generic.Order

@Suppress("ClassName", "PropertyName")
interface IORING_CAPABILITIES : Structure {
    @Order(0) var MaxVersion: IORING_VERSION
    @Order(1) var MaxSubmissionQueueSize: UINT32
    @Order(2) var MaxCompletionQueueSize: UINT32
    @Order(3) var FeatureFlags: IORING_FEATURE_FLAGS
}