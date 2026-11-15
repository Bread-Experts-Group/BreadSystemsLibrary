package ui.data

interface Point<T> {
    data class D2<T>(val x: T, val y: T) : Point<T>
}