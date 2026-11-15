package ui.data

interface DynamicValue<T> {
    val value: T

    data class Fixed<T>(override val value: T) : DynamicValue<T>
}