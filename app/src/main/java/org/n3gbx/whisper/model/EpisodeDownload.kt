package org.n3gbx.whisper.model

import java.time.LocalDateTime

data class EpisodeDownload(
    val episodeId: Identifier,
    val workId: String?,
    val progress: Int,
    val state: DownloadState,
    val lastUpdatedAt: LocalDateTime
)
