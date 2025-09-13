package org.n3gbx.whisper.ui.common.data

data class Item<T>(
    override val key: T,
    override val label: String
) : AbstractItem<T>()