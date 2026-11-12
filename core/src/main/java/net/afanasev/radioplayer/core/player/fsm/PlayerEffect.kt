package net.afanasev.radioplayer.core.player.fsm

internal sealed interface PlayerEffect {
    data object ReleaseController : PlayerEffect

    /**
     * stop, setMediaItem, play, prepare, in that order: playWhenReady may be false only while
     * the player is idle, which lets the reducer tell an outside pause from our own start.
     */
    data class StartPlayback(val streamUrl: String) : PlayerEffect

    /**
     * pause() + stop(): stop because a paused live stream goes stale, pause so playWhenReady is
     * false and the service can shut down.
     */
    data object StopPlayback : PlayerEffect

    /** stop() only: keeps playWhenReady true while reconnecting, see the reducer. */
    data object StopForRetry : PlayerEffect

    /** Cancels the previous fetch. */
    data class FetchNextTrack(val forTitle: String) : PlayerEffect

    data class StartConnectTimeout(val millis: Long) : PlayerEffect
    data object CancelConnectTimeout : PlayerEffect
    data class ScheduleRetry(val attempt: Int, val delayMs: Long) : PlayerEffect
    data object CancelRetry : PlayerEffect
    data class TrackAnalytics(val event: AnalyticsEvent) : PlayerEffect
}

internal sealed interface AnalyticsEvent {
    data class PlayButtonClick(val playback: Playback) : AnalyticsEvent
    data class PlaybackError(val reason: ErrorReason, val cause: Throwable?) : AnalyticsEvent
    data class Reconnect(val attempt: Int, val delayMs: Long) : AnalyticsEvent
}
