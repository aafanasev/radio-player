package net.afanasev.radioplayer.radioco

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StatusModel(
    @SerialName("current_track")
    val currentTrack: TrackModel,
)

@Serializable
internal data class NextTrackModel(
    @SerialName("next_track")
    val nextTrack: TrackModel,
)

@Serializable
internal data class TrackModel(
    @SerialName("title")
    val title: String,
    @SerialName("artwork_url_large")
    val artworkUri: String? = null,
)
