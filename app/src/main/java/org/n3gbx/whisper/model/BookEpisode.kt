package org.n3gbx.whisper.model

import org.n3gbx.whisper.Constants.UNSET_TIME

data class BookEpisode(
    val id: String,
    val bookId: String,
    val url: String,
    val duration: Long = UNSET_TIME,
    val playbackCache: BookEpisodePlaybackCache?
)
