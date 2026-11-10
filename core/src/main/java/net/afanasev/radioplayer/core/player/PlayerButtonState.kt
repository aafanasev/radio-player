package net.afanasev.radioplayer.core.player

import net.afanasev.radioplayer.core.player.fsm.Playback

enum class PlayerButtonState {
    PAUSED,
    LOADING,
    PLAYING,
}

fun Playback.toButtonState(): PlayerButtonState = when (this) {
    Playback.Disconnected,
    Playback.Connecting,
    Playback.Buffering,
    is Playback.Reconnecting -> PlayerButtonState.LOADING

    Playback.Idle,
    is Playback.Error -> PlayerButtonState.PAUSED

    Playback.Playing -> PlayerButtonState.PLAYING
}
