package net.afanasev.radioplayer.core.analytics

import net.afanasev.radioplayer.core.player.ErrorReason
import net.afanasev.radioplayer.core.player.Playback
import net.afanasev.radioplayer.core.theme.PlayerTheme

/**
 * Player-related event/error sink. Implement against your own analytics backend
 * (Firebase, Amplitude, ...); [NoOpPlayerAnalytics] is the default if you don't need one.
 * Both :core and the :radioco metadata provider report through this interface, so a single
 * implementation on the host [android.app.Application] captures both playback UI events and
 * metadata-fetch failures.
 */
interface PlayerAnalytics {

    /** The play button was tapped while playback was in [playback]. */
    fun onPlayButtonClick(playback: Playback)

    fun onThemeSelect(theme: PlayerTheme)

    /** A [NowPlayingMetadataProvider] fetch threw. [source] identifies the call, e.g. "artwork", "next_track". */
    fun onMetadataFetchError(source: String, throwable: Throwable)

    /** The metadata provider's artwork didn't match the currently playing title after retrying. */
    fun onArtworkMismatch(attempt: Int, maxAttempts: Int, retryDelayMs: Long)

    /** Playback ended up in [Playback.Error]. [cause] is null when the stream timed out. */
    fun onPlaybackError(reason: ErrorReason, cause: Throwable?) = Unit

    /** The stream failed and reconnect [attempt] is scheduled in [delayMs]. */
    fun onReconnect(attempt: Int, delayMs: Long) = Unit
}
