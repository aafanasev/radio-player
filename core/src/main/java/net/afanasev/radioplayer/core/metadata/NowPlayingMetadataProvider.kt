package net.afanasev.radioplayer.core.metadata

/**
 * Enriches now-playing state beyond what the stream's own ICY metadata carries.
 * Implement this against a station backend (e.g. the radio.co client module) to supply
 * artwork and next-track data; [core][net.afanasev.radioplayer.core] itself only requires
 * a stream URL and works with [NoOpNowPlayingMetadataProvider] if no backend is available.
 */
interface NowPlayingMetadataProvider {

    /** Returns an artwork URI for [expectedTitle], or null if unavailable/mismatched. */
    suspend fun fetchArtworkUri(expectedTitle: String): String?

    /** Returns the title of the next scheduled track, or null if unknown. */
    suspend fun fetchNextTrack(): String?
}
