package org.n3gbx.whisper.utils

import android.content.res.Resources
import org.n3gbx.whisper.model.StringResource

fun StringResource.asRawString(resources: Resources) : String = when (this) {
    is StringResource.SimpleStringResource -> this.str
    is StringResource.IdStringResource -> resources.getString(this.id, *this.args)
    is StringResource.IdArrayStringResource -> resources.getStringArray(this.id)[this.index]
    is StringResource.IdQuantityStringResource -> resources.getQuantityString(this.pluralId, this.quantity)
}