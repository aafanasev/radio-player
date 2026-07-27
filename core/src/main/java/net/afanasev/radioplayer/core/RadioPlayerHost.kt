package net.afanasev.radioplayer.core

import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider

/**
 * Implemented by the host [android.app.Application] to supply :core's extension points.
 * [PlaybackService] and [net.afanasev.radioplayer.core.player.PlayerViewModel] read this off
 * `application` at runtime, since Android constructs services/view-models without a way to
 * inject constructor arguments directly.
 */
interface RadioPlayerHost {
    val radioPlayerConfig: RadioPlayerConfig
    val nowPlayingMetadataProvider: NowPlayingMetadataProvider
    val playerAnalytics: PlayerAnalytics
}
