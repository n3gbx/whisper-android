package org.n3gbx.whisper.model

import android.net.Uri

sealed interface PlaybackSource {
    data class Resolved(val uri: Uri) : PlaybackSource
    data object Unresolved : PlaybackSource
}