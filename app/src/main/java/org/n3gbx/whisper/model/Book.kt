package org.n3gbx.whisper.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String?,
    val coverUrl: String?,
    val description: String?,
    val isBookmarked: Boolean,
    val currentEpisode: BookEpisode,
    val episodes: List<BookEpisode>
) {

    val currentEpisodeIndex: Int
        get() = episodes.indexOf(currentEpisode)

    fun matchesQuery(query: String?): Boolean {
        if (query.isNullOrBlank()) return true

        val text = query.lowercase()
        return title.lowercase().contains(text)
                || author.lowercase().contains(text)
                || description?.lowercase().orEmpty().contains(text)
    }
}
