package org.n3gbx.whisper.ui.common.utils

import androidx.media3.common.C
import kotlin.math.abs

fun Long?.convertToTime(): String {
    if (this == null || this == C.TIME_UNSET) {
        return "--:--"
    }

    val sec = abs(this / 1000)
    val minutes = sec / 60
    val seconds = sec % 60

    val minutesString = if (minutes < 10) {
        "0$minutes"
    } else {
        minutes.toString()
    }
    val secondsString = if (seconds < 10) {
        "0$seconds"
    } else {
        seconds.toString()
    }
    return "$minutesString:$secondsString"
}