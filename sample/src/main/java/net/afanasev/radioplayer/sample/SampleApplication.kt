package net.afanasev.radioplayer.sample

import android.app.Application
import net.afanasev.radioplayer.core.RadioPlayerConfig
import net.afanasev.radioplayer.core.RadioPlayerHost
import net.afanasev.radioplayer.core.analytics.NoOpPlayerAnalytics
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.metadata.NoOpNowPlayingMetadataProvider
import net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider

// OTON FM's radio.co stream (same as the otonfm app).
private const val STREAM_URL = "https://s4.radio.co/s696f24a77/listen"
private const val DEFAULT_ARTWORK_URI = "https://images.radio.co/station_logos/s696f24a77.20250505065303.jpg"

class SampleApplication : Application(), RadioPlayerHost {

    override val radioPlayerConfig = RadioPlayerConfig(
        streamUrl = STREAM_URL,
        stationName = "OTON FM (sample)",
        defaultArtworkUri = DEFAULT_ARTWORK_URI,
        sessionActivityClass = SampleActivity::class.java,
    )

    override val nowPlayingMetadataProvider: NowPlayingMetadataProvider = NoOpNowPlayingMetadataProvider

    override val playerAnalytics: PlayerAnalytics = NoOpPlayerAnalytics
}
