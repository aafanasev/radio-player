package net.afanasev.radioplayer.core

import android.app.Activity

/**
 * Branding and stream configuration for a whitelabel radio-player instance.
 * [sessionActivityClass] is the activity opened when the user taps the media notification.
 */
data class RadioPlayerConfig(
    val streamUrl: String,
    val stationName: String,
    val defaultArtworkUri: String,
    val sessionActivityClass: Class<out Activity>,
)
