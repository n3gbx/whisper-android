package org.n3gbx.whisper.ui.common.data

abstract class AbstractItem<T> {
    abstract val key: T
    abstract val label: String
}