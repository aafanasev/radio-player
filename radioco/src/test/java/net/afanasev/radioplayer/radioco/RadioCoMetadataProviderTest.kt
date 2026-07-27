package net.afanasev.radioplayer.radioco

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics
import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.theme.PlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val STATION_ID = "sTest123"

private class FakePlayerAnalytics : PlayerAnalytics {
    val metadataErrors = mutableListOf<Pair<String, Throwable>>()
    val artworkMismatches = mutableListOf<Int>()

    override fun onPlayButtonClick(state: PlayerButtonState) = Unit
    override fun onThemeSelect(theme: PlayerTheme) = Unit
    override fun onMetadataFetchError(source: String, throwable: Throwable) {
        metadataErrors += source to throwable
    }

    override fun onArtworkMismatch(attempt: Int, maxAttempts: Int, retryDelayMs: Long) {
        artworkMismatches += attempt
    }
}

private fun clientOf(engine: MockEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json() }
}

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

class RadioCoMetadataProviderTest {

    @Test
    fun `fetchArtworkUri returns artwork when title matches`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(true, request.url.encodedPath.contains(STATION_ID))
            respond(
                content = """{"current_track":{"title":"Now Playing","artwork_url_large":"https://example.com/art.jpg"}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val analytics = FakePlayerAnalytics()
        val provider = RadioCoMetadataProvider(STATION_ID, analytics, clientOf(engine))

        val artwork = provider.fetchArtworkUri("Now Playing")

        assertEquals("https://example.com/art.jpg", artwork)
        assertEquals(emptyList<Int>(), analytics.artworkMismatches)
    }

    @Test
    fun `fetchArtworkUri retries and gives up on persistent title mismatch`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"current_track":{"title":"Different Track","artwork_url_large":"https://example.com/art.jpg"}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val analytics = FakePlayerAnalytics()
        val provider = RadioCoMetadataProvider(STATION_ID, analytics, clientOf(engine))

        val artwork = provider.fetchArtworkUri("Expected Track")

        assertNull(artwork)
        assertEquals(listOf(1, 2, 3), analytics.artworkMismatches)
    }

    @Test
    fun `fetchArtworkUri reports and swallows HTTP errors`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val analytics = FakePlayerAnalytics()
        val provider = RadioCoMetadataProvider(STATION_ID, analytics, clientOf(engine))

        val artwork = provider.fetchArtworkUri("Any Title")

        assertNull(artwork)
        assertEquals(3, analytics.metadataErrors.count { it.first == "status" })
    }

    @Test
    fun `fetchNextTrack returns next track title`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"next_track":{"title":"Upcoming Song"}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val provider = RadioCoMetadataProvider(STATION_ID, FakePlayerAnalytics(), clientOf(engine))

        assertEquals("Upcoming Song", provider.fetchNextTrack())
    }

    @Test
    fun `fetchNextTrack reports and swallows errors`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val analytics = FakePlayerAnalytics()
        val provider = RadioCoMetadataProvider(STATION_ID, analytics, clientOf(engine))

        val nextTrack = provider.fetchNextTrack()

        assertNull(nextTrack)
        assertEquals(1, analytics.metadataErrors.count { it.first == "next_track" })
    }
}
