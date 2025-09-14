package org.n3gbx.whisper.model

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BookEpisode(
    val id: String,
    val url: String,
    val playbackCache: BookEpisodePlaybackCache?
) {
    suspend fun retrieveDurationTimeFromMetadata(): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(url, HashMap())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }
}
