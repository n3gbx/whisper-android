package org.n3gbx.whisper.model

import java.time.LocalDateTime

data class EpisodeProgress(
    val episodeId: Identifier,
    val time: Long,
    val lastUpdatedAt: LocalDateTime
)
