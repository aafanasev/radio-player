package net.afanasev.radioplayer.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import net.afanasev.radioplayer.core.player.AnalyticsEvent.PlayButtonClick
import net.afanasev.radioplayer.core.player.AnalyticsEvent.PlaybackError
import net.afanasev.radioplayer.core.player.AnalyticsEvent.Reconnect
import net.afanasev.radioplayer.core.player.PlayerEffect.CancelConnectTimeout
import net.afanasev.radioplayer.core.player.PlayerEffect.CancelRetry
import net.afanasev.radioplayer.core.player.PlayerEffect.FetchNextTrack
import net.afanasev.radioplayer.core.player.PlayerEffect.ReleaseController
import net.afanasev.radioplayer.core.player.PlayerEffect.ScheduleRetry
import net.afanasev.radioplayer.core.player.PlayerEffect.StartConnectTimeout
import net.afanasev.radioplayer.core.player.PlayerEffect.StartPlayback
import net.afanasev.radioplayer.core.player.PlayerEffect.StopPlayback
import net.afanasev.radioplayer.core.player.PlayerEffect.TrackAnalytics

/** Tuning for [reduce]. Internal on purpose: no host app has needed to change these yet. */
internal data class PlayerPolicy(
    val streamUrl: String,
    val connectTimeoutMs: Long = 15_000,
    val maxReconnectAttempts: Int = 5,
    val retryBaseDelayMs: Long = 2_000,
    val retryMaxDelayMs: Long = 30_000,
) {
    /** Exponential backoff: base, 2×base, 4×base, … capped at [retryMaxDelayMs]. */
    fun retryDelayMs(attempt: Int): Long =
        (retryBaseDelayMs shl (attempt - 1).coerceIn(0, 20)).coerceAtMost(retryMaxDelayMs)
}

internal data class Next(
    val state: PlayerState,
    val effects: List<PlayerEffect> = emptyList(),
)

/** Pure transition function: no I/O, no clocks, no Android calls. */
internal fun reduce(state: PlayerState, event: PlayerEvent, policy: PlayerPolicy): Next =
    when (event) {
        PlayerEvent.PlayPauseClicked -> onPlayPause(state, policy)

        PlayerEvent.RetryClicked -> when (state.playback) {
            Playback.Idle, is Playback.Error -> start(state, policy)
            else -> Next(state)
        }

        is PlayerEvent.ControllerConnected -> onControllerConnected(state, event)

        is PlayerEvent.ControllerFailed -> {
            val analytics = TrackAnalytics(PlaybackError(ErrorReason.CONTROLLER, event.cause))
            when (state.playback) {
                // Nothing to show the user while playback was neither wanted nor reachable.
                Playback.Disconnected, Playback.Connecting, is Playback.Reconnecting -> Next(
                    state.copy(playback = Playback.Error(ErrorReason.CONTROLLER)),
                    listOf(CancelConnectTimeout, CancelRetry, analytics),
                )

                else -> Next(state, listOf(analytics))
            }
        }

        PlayerEvent.ServiceDestroyed -> Next(
            state.copy(playback = Playback.Disconnected),
            listOf(CancelConnectTimeout, CancelRetry, ReleaseController),
        )

        is PlayerEvent.PlayerChanged -> onPlayerChanged(state, event.snapshot, policy)

        is PlayerEvent.MetadataChanged -> {
            val (nowPlaying, effects) = applyMetadata(state.nowPlaying, event.metadata)
            Next(state.copy(nowPlaying = nowPlaying), effects)
        }

        is PlayerEvent.NextTrackLoaded ->
            if (event.forTitle == state.nowPlaying.title) {
                Next(state.copy(nowPlaying = state.nowPlaying.copy(nextTrackTitle = event.nextTitle.orEmpty())))
            } else {
                Next(state) // stale: the track changed while fetching
            }

        PlayerEvent.ConnectTimedOut -> when (val p = state.playback) {
            Playback.Connecting -> reconnectOrFail(state, 0, cause = null, stop = true, policy)
            is Playback.Reconnecting -> reconnectOrFail(state, p.attempt, cause = null, stop = true, policy)
            else -> Next(state)
        }

        is PlayerEvent.RetryDue -> {
            val p = state.playback
            if (p is Playback.Reconnecting && p.attempt == event.attempt) {
                Next(state, listOf(StartPlayback(policy.streamUrl), StartConnectTimeout(policy.connectTimeoutMs)))
            } else {
                Next(state) // stale: cancelled or superseded
            }
        }
    }

private fun onPlayPause(state: PlayerState, policy: PlayerPolicy): Next {
    val click = TrackAnalytics(PlayButtonClick(state.playback))
    return when (state.playback) {
        Playback.Disconnected -> Next(state)
        Playback.Idle, is Playback.Error -> start(state, policy)
        Playback.Connecting, Playback.Buffering, is Playback.Reconnecting -> Next(
            state.copy(playback = Playback.Idle),
            listOf(StopPlayback, CancelConnectTimeout, CancelRetry, click),
        )

        Playback.Playing -> Next(state.copy(playback = Playback.Idle), listOf(StopPlayback, click))
    }
}

private fun start(state: PlayerState, policy: PlayerPolicy) = Next(
    state.copy(playback = Playback.Connecting),
    listOf(
        StartPlayback(policy.streamUrl),
        StartConnectTimeout(policy.connectTimeoutMs),
        TrackAnalytics(PlayButtonClick(state.playback)),
    ),
)

private fun onControllerConnected(state: PlayerState, event: PlayerEvent.ControllerConnected): Next {
    val isPlaying = event.snapshot.isPlaying
    val playback = when (val p = state.playback) {
        Playback.Disconnected -> if (isPlaying) Playback.Playing else Playback.Idle
        Playback.Idle, is Playback.Error -> if (isPlaying) Playback.Playing else p
        // The service listener is the source of truth while playback is active.
        else -> p
    }
    val (nowPlaying, effects) = applyMetadata(state.nowPlaying, event.metadata)
    return Next(PlayerState(playback, nowPlaying), effects)
}

private fun onPlayerChanged(state: PlayerState, s: PlayerSnapshot, policy: PlayerPolicy): Next {
    val p = state.playback
    if (s.error != null) {
        return if (p.isActive) onError(state, s.error, policy) else Next(state)
    }

    fun to(playback: Playback, vararg effects: PlayerEffect) =
        Next(state.copy(playback = playback), effects.toList())

    val stoppedFromOutside = !s.playWhenReady || s.playbackState == Player.STATE_IDLE
    return when (p) {
        // The controller's own snapshot (ControllerConnected) decides where we start.
        Playback.Disconnected -> Next(state)

        // Started from outside: notification, Bluetooth, Android Auto.
        Playback.Idle, is Playback.Error -> when {
            s.isPlaying -> to(Playback.Playing)
            s.playWhenReady && s.playbackState == Player.STATE_BUFFERING -> to(Playback.Buffering)
            else -> Next(state)
        }

        // Our own start goes through several intermediate snapshots (setMediaItem, prepare,
        // play), so only "it plays" moves us on; failures arrive as errors or the timeout.
        Playback.Connecting, is Playback.Reconnecting ->
            if (s.isPlaying) to(Playback.Playing, CancelConnectTimeout, CancelRetry) else Next(state)

        Playback.Playing -> when {
            s.isPlaying -> Next(state)
            stoppedFromOutside -> to(Playback.Idle)
            // A live stream "ends" when the server drops the connection.
            s.playbackState == Player.STATE_ENDED -> reconnectOrFail(state, 0, null, stop = false, policy)
            s.playbackState == Player.STATE_BUFFERING -> to(Playback.Buffering)
            else -> Next(state) // e.g. suppressed by a transient audio-focus loss
        }

        Playback.Buffering -> when {
            s.isPlaying -> to(Playback.Playing)
            stoppedFromOutside -> to(Playback.Idle)
            s.playbackState == Player.STATE_ENDED -> reconnectOrFail(state, 0, null, stop = false, policy)
            else -> Next(state)
        }
    }
}

private fun onError(state: PlayerState, error: PlaybackException, policy: PlayerPolicy): Next {
    val reason = error.toErrorReason()
    if (reason == ErrorReason.NETWORK) {
        val attempt = (state.playback as? Playback.Reconnecting)?.attempt ?: 0
        return reconnectOrFail(state, attempt, error, stop = false, policy)
    }
    return Next(
        state.copy(playback = Playback.Error(reason)),
        listOf(CancelConnectTimeout, CancelRetry, TrackAnalytics(PlaybackError(reason, error))),
    )
}

/** [attempt] is the number of reconnects already made; 0 when the failure hit a healthy stream. */
private fun reconnectOrFail(
    state: PlayerState,
    attempt: Int,
    cause: Throwable?,
    stop: Boolean,
    policy: PlayerPolicy,
): Next {
    val stopEffects = if (stop) listOf(StopPlayback) else emptyList()
    if (attempt >= policy.maxReconnectAttempts) {
        return Next(
            state.copy(playback = Playback.Error(ErrorReason.NETWORK)),
            stopEffects + listOf(
                CancelConnectTimeout,
                CancelRetry,
                TrackAnalytics(PlaybackError(ErrorReason.NETWORK, cause)),
            ),
        )
    }
    val next = attempt + 1
    val delayMs = policy.retryDelayMs(next)
    return Next(
        state.copy(playback = Playback.Reconnecting(next)),
        stopEffects + listOf(
            CancelConnectTimeout,
            ScheduleRetry(next, delayMs),
            TrackAnalytics(Reconnect(next, delayMs)),
        ),
    )
}

/**
 * A missing title keeps the current now-playing info: the player clears its metadata on
 * stop/re-prepare, and the screen shouldn't go blank in between. A new title resets the
 * next-track line and fetches it again.
 */
private fun applyMetadata(current: NowPlaying, metadata: TrackMetadata): Pair<NowPlaying, List<PlayerEffect>> {
    val title = metadata.title?.takeIf { it.isNotBlank() } ?: return current to emptyList()
    return if (title != current.title) {
        NowPlaying(title = title, artworkUri = metadata.artworkUri) to listOf(FetchNextTrack(title))
    } else {
        current.copy(artworkUri = metadata.artworkUri ?: current.artworkUri) to emptyList()
    }
}

internal fun PlaybackException.toErrorReason(): ErrorReason = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> ErrorReason.SOURCE

    in 2000..2999, // ERROR_CODE_IO_*: network failures, bad HTTP status, ...
    PlaybackException.ERROR_CODE_TIMEOUT,
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> ErrorReason.NETWORK

    PlaybackException.ERROR_CODE_UNSPECIFIED -> ErrorReason.UNKNOWN
    else -> ErrorReason.SOURCE
}
