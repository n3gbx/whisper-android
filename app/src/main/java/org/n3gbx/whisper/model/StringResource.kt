package org.n3gbx.whisper.model

import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

@Serializable
sealed class StringResource {
    companion object {
        fun fromStr(str: String): StringResource = SimpleStringResource(str)
        fun fromRes(@StringRes id: Int, vararg args: Any): StringResource = IdStringResource(id, *args)
        fun fromArrayRes(@ArrayRes id: Int, index: Int): StringResource = IdArrayStringResource(id, index)
        fun fromPluralRes(@PluralsRes id: Int, plural: Int): StringResource = IdQuantityStringResource(id, plural)
        fun empty(): StringResource = SimpleStringResource("")
    }

    class SimpleStringResource(val str: String) : StringResource()

    class IdStringResource(@StringRes val id: Int, vararg val args: Any) : StringResource()

    class IdArrayStringResource(@ArrayRes val id: Int, val index: Int) : StringResource()

    class IdQuantityStringResource(@PluralsRes val pluralId: Int, val quantity: Int) : StringResource()
}