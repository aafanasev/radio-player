package net.afanasev.radioplayer.core.player

/**
 * Everything the UI needs to render the player. Playback status and now-playing info are
 * independent, so the track info survives pauses and errors.
 */
data class PlayerState(
    val playback: Playback = Playback.Disconnected,
    val nowPlaying: NowPlaying = NowPlaying(),
)

sealed interface Playback {
    /** No `MediaController` is connected yet. */
    data object Disconnected : Playback

    /** Connected but not playing. For live radio, pause and stop mean the same thing. */
    data object Idle : Playback

    /** The user asked to play; waiting for the first audio. */
    data object Connecting : Playback

    data object Playing : Playback

    /** Was playing, but the stream stalled. */
    data object Buffering : Playback

    /** The stream failed; waiting to retry, or retry [attempt] is in progress. */
    data class Reconnecting(val attempt: Int) : Playback

    data class Error(val reason: ErrorReason) : Playback
}

enum class ErrorReason { NETWORK, SOURCE, CONTROLLER, UNKNOWN }

/** True while playback is wanted, i.e. the player is playing or trying to. */
val Playback.isActive: Boolean
    get() = this == Playback.Connecting ||
        this == Playback.Playing ||
        this == Playback.Buffering ||
        this is Playback.Reconnecting

data class NowPlaying(
    val title: String = "",
    /** Null means "use [net.afanasev.radioplayer.core.RadioPlayerConfig.defaultArtworkUri]". */
    val artworkUri: String? = null,
    val nextTrackTitle: String = "",
)

fun Playback.toButtonState(): PlayerButtonState = when (this) {
    Playback.Disconnected,
    Playback.Connecting,
    Playback.Buffering,
    is Playback.Reconnecting -> PlayerButtonState.LOADING

    Playback.Idle,
    is Playback.Error -> PlayerButtonState.PAUSED

    Playback.Playing -> PlayerButtonState.PLAYING
}
