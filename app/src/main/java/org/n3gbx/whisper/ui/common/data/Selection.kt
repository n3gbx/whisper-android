package org.n3gbx.whisper.ui.common.data

data class MultiSelection<T>(val value: List<T> = emptyList()) : Selection<T> {
    override fun contains(value: T): Boolean = this.value.contains(value)
}

data class SingleSelection<T>(val value: T? = null) : Selection<T> {
    override fun contains(value: T): Boolean = this.value == value
}

sealed interface Selection<T> {
    fun contains(value: T): Boolean
}

enum class SelectionType {
    Single, Multi
}

operator fun <T> MultiSelection<T>.minus(element: T): MultiSelection<T> {
    val result = ArrayList<T>(value.size)
    var removed = false
    val newValue = value.filterTo(result) {
        if (!removed && it == element) { removed = true; false } else true
    }
    return MultiSelection(newValue)
}

operator fun <T> MultiSelection<T>.plus(element: T): MultiSelection<T> {
    val result = ArrayList<T>(value.size + 1)
    result.addAll(value)
    result.add(element)
    return MultiSelection(result)
}