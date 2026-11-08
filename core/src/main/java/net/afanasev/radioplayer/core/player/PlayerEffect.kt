package net.afanasev.radioplayer.core.player

internal sealed interface PlayerEffect {
    data object ReleaseController : PlayerEffect

    /** setMediaItem + prepare + play. */
    data class StartPlayback(val streamUrl: String) : PlayerEffect

    /** stop(), not just pause(): a paused live stream goes stale. */
    data object StopPlayback : PlayerEffect

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
