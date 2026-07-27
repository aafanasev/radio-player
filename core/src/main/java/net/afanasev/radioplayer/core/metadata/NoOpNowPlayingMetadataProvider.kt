package net.afanasev.radioplayer.core.metadata

/**
 * Default [NowPlayingMetadataProvider]: no enrichment beyond the stream's own ICY metadata,
 * which ExoPlayer already surfaces as [androidx.media3.common.MediaMetadata.title].
 */
object NoOpNowPlayingMetadataProvider : NowPlayingMetadataProvider {
    override suspend fun fetchArtworkUri(expectedTitle: String): String? = null
    override suspend fun fetchNextTrack(): String? = null
}
