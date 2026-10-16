package org.bread_experts_group.bread_systems_library.generic

abstract class ValueProvider(val value: Any?) {
    override fun toString(): String = "$value"
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = other is ValueProvider && this.value == other.value
}