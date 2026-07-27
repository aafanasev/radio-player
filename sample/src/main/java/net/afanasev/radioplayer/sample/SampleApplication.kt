package net.afanasev.radioplayer.sample

import android.app.Application
import net.afanasev.radioplayer.core.RadioPlayerConfig
import net.afanasev.radioplayer.core.RadioPlayerHost
import net.afanasev.radioplayer.core.analytics.NoOpPlayerAnalytics
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.metadata.NoOpNowPlayingMetadataProvider
import net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider

// SomaFM's Groove Salad: a public stream that carries ICY title metadata, so the default
// NoOpNowPlayingMetadataProvider still shows a real "now playing" title with zero backend.
private const val STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3"
private const val DEFAULT_ARTWORK_URI = "https://api.somafm.com/logos/512x512/groovesalad512.png"

class SampleApplication : Application(), RadioPlayerHost {

    override val radioPlayerConfig = RadioPlayerConfig(
        streamUrl = STREAM_URL,
        stationName = "Groove Salad (sample)",
        defaultArtworkUri = DEFAULT_ARTWORK_URI,
        sessionActivityClass = SampleActivity::class.java,
    )

    override val nowPlayingMetadataProvider: NowPlayingMetadataProvider = NoOpNowPlayingMetadataProvider

    override val playerAnalytics: PlayerAnalytics = NoOpPlayerAnalytics
}
