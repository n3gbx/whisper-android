package org.n3gbx.whisper.ui.common.utils

import androidx.media3.common.C
import org.n3gbx.whisper.Constants.UNSET_TIME_PLACEHOLDER
import kotlin.math.abs

fun Long?.convertToTime(): String {
    if (this == null || this == C.TIME_UNSET || this < 0) {
        return UNSET_TIME_PLACEHOLDER
    }

    val totalSeconds = abs(this / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val hoursString = if (hours < 10) "0$hours" else hours.toString()
    val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()
    val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()

    return if (hours > 0) {
        "$hoursString:$minutesString:$secondsString"
    } else {
        "$minutesString:$secondsString"
    }
}
