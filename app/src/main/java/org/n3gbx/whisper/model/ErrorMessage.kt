package org.n3gbx.whisper.model

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException

class ErrorMessage(throwable: Throwable?) {

    val text: String = when (throwable) {
        is SQLiteConstraintException -> {
            if (throwable.message.orEmpty().startsWith("UNIQUE")) {
                "Duplicate data error"
            } else {
                "Internal database error"
            }
        }
        is SQLiteException -> "Internal database error"
        is IllegalArgumentException -> "Invalid argument"
        is IllegalStateException -> "Initialization error"
        is NullPointerException -> "Data does not exist"
        else -> "Oops! Something went wrong"
    }
}