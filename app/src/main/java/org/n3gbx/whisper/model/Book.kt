package org.n3gbx.whisper.model

import org.n3gbx.whisper.Constants.UNSET_TIME
import kotlin.math.roundToInt

data class Book(
    val id: Identifier,
    val title: String,
    val author: String,
    val narrator: String?,
    val coverUrl: String?,
    val description: String?,
    val isBookmarked: Boolean,
    val recentEpisode: Episode,
    val episodes: List<Episode>
) {
    val isFinished: Boolean
        get() = episodes.all { it.isFinished }

    val isStarted: Boolean
        get() = progressValue > 0f && !isFinished

    val recentEpisodeIndex: Int
        get() = episodes.indexOf(recentEpisode)

    val progressPercentage: Int
        get() = (progressValue * 100).roundToInt()

    val progressValue: Float
        get() = episodes.fold(0f) { acc, e -> acc + e.progressValue } / episodes.size.toFloat()

    val totalDuration: Long
        get() = episodes.filter { it.duration != UNSET_TIME }.sumOf { it.duration }

    fun matchesQuery(query: String?): Boolean {
        if (query.isNullOrBlank()) return true

        val text = query.lowercase()
        return title.lowercase().contains(text)
                || author.lowercase().contains(text)
    }
}
