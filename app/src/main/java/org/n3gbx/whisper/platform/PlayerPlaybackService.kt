package org.n3gbx.whisper.platform

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableSharedFlow
import org.n3gbx.whisper.ui.utils.Timer

class PlayerPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val sleepTimer by lazy { Timer() }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()
    }

    override fun onDestroy() {
        sleepTimer.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    // Stop the service in any case when the user dismisses the app
    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimer.cancel()
        sleepTimer.start(durationMs) {
            mediaSession?.player?.pause()
        }
    }

    private fun cancelSleepTimer() {
        sleepTimer.cancel()
    }

    private inner class SessionCallback : MediaSession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CustomCommand.START_SLEEP_TIMER.name, Bundle.EMPTY))
                .add(SessionCommand(CustomCommand.CANCEL_SLEEP_TIMER.name, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                CustomCommand.START_SLEEP_TIMER.name -> {
                    mediaSession?.player?.let { player ->
                        val invalidDuration = -1L
                        val commandDuration = customCommand.customExtras.getLong("duration", invalidDuration)

                        val duration = if (commandDuration == invalidDuration) {
                            player.duration - player.currentPosition
                        } else {
                            commandDuration
                        }

                        if (duration > 0) {
                            startSleepTimer(duration)
                        }
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CustomCommand.CANCEL_SLEEP_TIMER.name -> {
                    cancelSleepTimer()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    enum class CustomCommand {
        START_SLEEP_TIMER, CANCEL_SLEEP_TIMER
    }
}