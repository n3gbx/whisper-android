package org.n3gbx.whisper.core.common

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.core.Constants.UNSET_TIME
import javax.inject.Inject
import kotlin.coroutines.resume

interface EpisodeDurationProber {
    suspend fun probe(url: String): Long
}

enum class EpisodeDurationProberType {
    MEDIA_EXO_PLAYER,
    MEDIA_METADATA_RETRIEVER
}

class EpisodeDurationProberContext @Inject constructor(
    private val mediaExoPlayerProber: MediaExoPlayerEpisodeDurationProber,
    private val mediaMetadataRetrieverProber: MediaMetadataRetrieverEpisodeDurationProber,
) {

    private var strategy: EpisodeDurationProber? = null

    fun setStrategy(strategyType: EpisodeDurationProberType): EpisodeDurationProberContext {
        when (strategyType) {
            EpisodeDurationProberType.MEDIA_EXO_PLAYER ->
                this.strategy = mediaExoPlayerProber
            EpisodeDurationProberType.MEDIA_METADATA_RETRIEVER ->
                this.strategy = mediaMetadataRetrieverProber
        }
        return this
    }

    suspend fun executeStrategy(url: String) =
        strategy?.probe(url) ?: UNSET_TIME
}

class MediaExoPlayerEpisodeDurationProber @Inject constructor(
    @ApplicationContext private val context: Context
) : EpisodeDurationProber {

    override suspend fun probe(url: String) =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val player = ExoPlayer.Builder(context).build()

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            val duration = player.duration
                            player.release()
                            continuation.resume(
                                if (duration > 0) duration else UNSET_TIME
                            )
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        player.release()
                        continuation.resume(UNSET_TIME)
                    }
                })

                continuation.invokeOnCancellation {
                    player.release()
                }

                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
            }
        }
}

class MediaMetadataRetrieverEpisodeDurationProber @Inject constructor() : EpisodeDurationProber {

    override suspend fun probe(url: String) =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(url, HashMap())
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong()
                        ?.let { continuation.resume(it) }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    continuation.resume(UNSET_TIME)
                } finally {
                    retriever.release()
                }

                continuation.invokeOnCancellation {
                    retriever.release()
                }
            }
        }
}

