package org.n3gbx.whisper.model

import java.time.LocalDateTime

data class BookEpisodeProgress(
    val id: String,
    val episodeId: Identifier,
    val lastTime: Long,
    val lastUpdatedAt: LocalDateTime
)
