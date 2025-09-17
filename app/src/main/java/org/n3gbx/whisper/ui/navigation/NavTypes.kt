package org.n3gbx.whisper.ui.navigation

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.n3gbx.whisper.model.Identifier

object IdentifierNavType : NavType<Identifier?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): Identifier? {
        return bundle.getString(key)?.let { parseValue(it) }
    }

    override fun put(bundle: Bundle, key: String, value: Identifier?) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): Identifier {
        return Json.decodeFromString(value)
    }

    override fun serializeAsValue(value: Identifier?): String {
        return Json.encodeToString(value)
    }
}