package net.afanasev.radioplayer.radioco

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import net.afanasev.radioplayer.core.analytics.NoOpPlayerAnalytics
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider

private const val TIMEOUT_MS = 6_000L
private const val RETRY_MAX_COUNT = 3
private const val RETRY_DELAY_MS = 2_000L

/**
 * [NowPlayingMetadataProvider] backed by the [radio.co](https://radio.co) public status API.
 * [stationId] is the id from your station's radio.co dashboard URL/embed code (e.g. `s696f24a77`).
 */
class RadioCoMetadataProvider(
    private val stationId: String,
    private val analytics: PlayerAnalytics = NoOpPlayerAnalytics,
    private val httpClient: HttpClient = defaultHttpClient(),
) : NowPlayingMetadataProvider {

    private val statusUrl = "https://public.radio.co/stations/$stationId/status?v="
    private val nextTrackUrl = "https://public.radio.co/stations/$stationId/next?v="

    override suspend fun fetchArtworkUri(expectedTitle: String): String? {
        repeat(RETRY_MAX_COUNT) { attemptIdx ->
            loadStatus()?.let {
                if (it.currentTrack.title == expectedTitle) {
                    return it.currentTrack.artworkUri
                }
            }
            analytics.onArtworkMismatch(attemptIdx + 1, RETRY_MAX_COUNT, RETRY_DELAY_MS)
            delay(RETRY_DELAY_MS)
        }

        return null
    }

    private suspend fun loadStatus(): StatusModel? {
        return try {
            httpClient.get(statusUrl + System.currentTimeMillis()).body()
        } catch (e: Exception) {
            analytics.onMetadataFetchError("status", e)
            null
        }
    }

    override suspend fun fetchNextTrack(): String? {
        return try {
            val response: NextTrackModel = httpClient.get(nextTrackUrl + System.currentTimeMillis()).body()
            response.nextTrack.title
        } catch (e: Exception) {
            analytics.onMetadataFetchError("next_track", e)
            null
        }
    }
}

private fun defaultHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = TIMEOUT_MS
        connectTimeoutMillis = TIMEOUT_MS
        socketTimeoutMillis = TIMEOUT_MS
    }
}
