package org.n3gbx.whisper.model

import org.n3gbx.whisper.Constants.UNSET_TIME

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String?,
    val coverUrl: String?,
    val description: String?,
    val isBookmarked: Boolean,
    val recentEpisode: BookEpisode,
    val episodes: List<BookEpisode>
) {

    val recentEpisodeIndex: Int
        get() = episodes.indexOf(recentEpisode)

    val progressPercentage: Int
        get() = episodes.sumOf { it.playbackCache?.progressPercentage ?: 0 }

    val progressValue: Float
        get() = progressPercentage.toFloat() / 100

    val totalDuration: Long
        get() = episodes.filter { it.duration != UNSET_TIME }.sumOf { it.duration }

    fun matchesQuery(query: String?): Boolean {
        if (query.isNullOrBlank()) return true

        val text = query.lowercase()
        return title.lowercase().contains(text)
                || author.lowercase().contains(text)
                || description?.lowercase().orEmpty().contains(text)
    }
}
