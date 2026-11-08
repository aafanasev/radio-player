package net.afanasev.radioplayer.core.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.afanasev.radioplayer.core.player.ErrorReason
import net.afanasev.radioplayer.core.player.Playback

/**
 * One line describing [playback] when it needs explaining (reconnecting, errors), empty
 * otherwise. [text] returns plain text rather than a string resource, so callers can localize
 * it however they like; it defaults to [defaultPlaybackStatusText].
 */
@Composable
fun PlaybackStatus(
    playback: Playback,
    modifier: Modifier = Modifier,
    text: (Playback) -> String? = ::defaultPlaybackStatusText,
) {
    Text(
        text = text(playback).orEmpty(),
        textAlign = TextAlign.Center,
        minLines = 1,
        maxLines = 2,
        style = MaterialTheme.typography.bodyMedium,
        color = if (playback is Playback.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

fun defaultPlaybackStatusText(playback: Playback): String? = when (playback) {
    is Playback.Reconnecting -> "Connection lost. Reconnecting…"
    is Playback.Error -> when (playback.reason) {
        ErrorReason.NETWORK -> "Can't reach the stream. Check your connection and tap play to retry."
        ErrorReason.SOURCE -> "The stream can't be played right now. Tap play to retry."
        ErrorReason.CONTROLLER -> "Couldn't start the player. Tap play to retry."
        ErrorReason.UNKNOWN -> "Something went wrong. Tap play to retry."
    }

    else -> null
}

@Preview
@Composable
private fun PlaybackStatusErrorPreview() {
    PlaybackStatus(playback = Playback.Error(ErrorReason.NETWORK))
}
