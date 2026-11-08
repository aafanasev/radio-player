package net.afanasev.radioplayer.core.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider

/** Runs [PlayerEffect]s. Split out from [net.afanasev.radioplayer.core.RadioPlayer] so tests can fake it. */
internal interface PlayerEffects {
    fun handle(effect: PlayerEffect)

    /**
     * Whether a `MediaController` should be connected: while the UI observes the state or
     * playback is active. Otherwise it's released so the service can shut down.
     */
    fun setControllerNeeded(needed: Boolean)
}

internal class PlayerEffectHandler(
    private val context: Context,
    private val metadata: NowPlayingMetadataProvider,
    private val analytics: PlayerAnalytics,
    private val scope: CoroutineScope,
    private val send: (PlayerEvent) -> Unit,
) : PlayerEffects {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controllerNeeded = false

    private var commandJob: Job? = null
    private var nextTrackJob: Job? = null
    private var timeoutJob: Job? = null
    private var retryJob: Job? = null

    override fun handle(effect: PlayerEffect) {
        when (effect) {
            PlayerEffect.ReleaseController -> {
                releaseController()
                // The service went away under an observing UI: reconnect, which recreates it.
                if (controllerNeeded) connectController()
            }

            is PlayerEffect.StartPlayback -> command {
                it.setMediaItem(MediaItem.fromUri(effect.streamUrl))
                it.prepare()
                it.play()
            }

            PlayerEffect.StopPlayback -> command {
                // pause() as well, so playWhenReady is false and onTaskRemoved lets the service stop.
                it.pause()
                it.stop()
            }

            is PlayerEffect.FetchNextTrack -> {
                nextTrackJob?.cancel()
                nextTrackJob = scope.launch {
                    val next = try {
                        metadata.fetchNextTrack()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        analytics.onMetadataFetchError("next_track", e)
                        null
                    }
                    send(PlayerEvent.NextTrackLoaded(effect.forTitle, next))
                }
            }

            is PlayerEffect.StartConnectTimeout -> {
                timeoutJob?.cancel()
                timeoutJob = scope.launch {
                    delay(effect.millis)
                    send(PlayerEvent.ConnectTimedOut)
                }
            }

            PlayerEffect.CancelConnectTimeout -> {
                timeoutJob?.cancel()
                timeoutJob = null
            }

            is PlayerEffect.ScheduleRetry -> {
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(effect.delayMs)
                    send(PlayerEvent.RetryDue(effect.attempt))
                }
            }

            PlayerEffect.CancelRetry -> {
                retryJob?.cancel()
                retryJob = null
            }

            is PlayerEffect.TrackAnalytics -> when (val event = effect.event) {
                is AnalyticsEvent.PlayButtonClick -> analytics.onPlayButtonClick(event.playback)
                is AnalyticsEvent.PlaybackError -> analytics.onPlaybackError(event.reason, event.cause)
                is AnalyticsEvent.Reconnect -> analytics.onReconnect(event.attempt, event.delayMs)
            }
        }
    }

    override fun setControllerNeeded(needed: Boolean) {
        if (needed == controllerNeeded) return
        controllerNeeded = needed
        if (needed) connectController() else releaseController()
    }

    /**
     * Runs [block] once a controller is available, connecting on demand. A newer command
     * cancels one still waiting for the connection, so a quick play-then-cancel can't play.
     */
    private fun command(block: (MediaController) -> Unit) {
        val previous = commandJob
        commandJob = scope.launch {
            previous?.cancelAndJoin()
            awaitController()?.let(block)
        }
    }

    private suspend fun awaitController(): MediaController? {
        val future = connectController()
        return try {
            future.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // reported as ControllerFailed by connectController's callback
        }
    }

    private fun connectController(): ListenableFuture<MediaController> {
        controllerFuture?.let { return it }

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        lateinit var future: ListenableFuture<MediaController>
        future = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (controllerFuture === future) controllerFuture = null
                }
            })
            .buildAsync()
        controllerFuture = future

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(controller: MediaController) {
                    if (controllerFuture !== future) return
                    send(
                        PlayerEvent.ControllerConnected(
                            snapshot = controller.toSnapshot(),
                            metadata = controller.mediaMetadata.toTrackMetadata(),
                        )
                    )
                }

                override fun onFailure(t: Throwable) {
                    if (controllerFuture !== future) return // released on purpose
                    controllerFuture = null
                    send(PlayerEvent.ControllerFailed(t))
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        return future
    }

    private fun releaseController() {
        commandJob?.cancel()
        commandJob = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }
}
