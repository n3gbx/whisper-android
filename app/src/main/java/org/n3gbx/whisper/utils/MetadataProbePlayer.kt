package org.n3gbx.whisper.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.Constants.UNSET_TIME
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MetadataProbePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun probeDuration(url: String): Long =
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