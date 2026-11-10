package net.afanasev.radioplayer.core.analytics

import net.afanasev.radioplayer.core.player.fsm.Playback
import net.afanasev.radioplayer.core.theme.PlayerTheme

object NoOpPlayerAnalytics : PlayerAnalytics {
    override fun onPlayButtonClick(playback: Playback) = Unit
    override fun onThemeSelect(theme: PlayerTheme) = Unit
    override fun onMetadataFetchError(source: String, throwable: Throwable) = Unit
    override fun onArtworkMismatch(attempt: Int, maxAttempts: Int, retryDelayMs: Long) = Unit
}
