package org.bread_experts_group.bread_systems_library.ffm

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Critical(val heapAccess: Boolean)