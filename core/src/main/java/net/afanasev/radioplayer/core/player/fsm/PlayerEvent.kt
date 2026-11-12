package net.afanasev.radioplayer.core.player.fsm

import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

sealed interface PlayerEvent {
    // user
    /** Play button tapped: starts from Idle or Error (retry), stops or cancels otherwise. */
    data object PlayPauseClicked : PlayerEvent

    // controller / service
    data class ControllerConnected(val snapshot: PlayerSnapshot, val metadata: TrackMetadata) : PlayerEvent
    data class ControllerFailed(val cause: Throwable) : PlayerEvent
    data object ServiceDestroyed : PlayerEvent

    // Player.Listener
    data class PlayerChanged(val snapshot: PlayerSnapshot) : PlayerEvent
    data class MetadataChanged(val metadata: TrackMetadata) : PlayerEvent

    // effect results
    data class NextTrackLoaded(val forTitle: String, val nextTitle: String?) : PlayerEvent
    data object ConnectTimedOut : PlayerEvent
    data class RetryDue(val attempt: Int) : PlayerEvent
}

/**
 * A consistent view of the player taken once per `Player.Listener.onEvents` batch.
 * [error] is set only in the batch where the error was raised, so a later batch that still
 * carries the same `Player.playerError` is not counted as a second failure.
 */
data class PlayerSnapshot(
    val playWhenReady: Boolean,
    /** One of `Player.STATE_*`. */
    val playbackState: Int,
    val isPlaying: Boolean,
    val error: PlaybackException? = null,
)

data class TrackMetadata(val title: String?, val artworkUri: String?)

internal fun Player.toSnapshot(error: PlaybackException? = playerError) =
    PlayerSnapshot(
        playWhenReady = playWhenReady,
        playbackState = playbackState,
        isPlaying = isPlaying,
        error = error,
    )

internal fun MediaMetadata.toTrackMetadata() =
    TrackMetadata(title = title?.toString(), artworkUri = artworkUri?.toString())
