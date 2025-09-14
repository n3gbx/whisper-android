package org.n3gbx.whisper.data.dto

data class BookDto(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val narrator: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val episodes: List<BookEpisodeDto> = emptyList()
)
