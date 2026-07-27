package net.afanasev.radioplayer.core.analytics

import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.theme.PlayerTheme

/**
 * Player-related event/error sink. Implement against your own analytics backend
 * (Firebase, Amplitude, ...); [NoOpPlayerAnalytics] is the default if you don't need one.
 * Both :core and the :radioco metadata provider report through this interface, so a single
 * implementation on the host [android.app.Application] captures both playback UI events and
 * metadata-fetch failures.
 */
interface PlayerAnalytics {

    fun onPlayButtonClick(state: PlayerButtonState)

    fun onThemeSelect(theme: PlayerTheme)

    /** A [NowPlayingMetadataProvider] fetch threw. [source] identifies the call, e.g. "artwork", "next_track". */
    fun onMetadataFetchError(source: String, throwable: Throwable)

    /** The metadata provider's artwork didn't match the currently playing title after retrying. */
    fun onArtworkMismatch(attempt: Int, maxAttempts: Int, retryDelayMs: Long)
}
