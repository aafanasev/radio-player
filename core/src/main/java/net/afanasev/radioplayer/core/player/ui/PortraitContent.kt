package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.afanasev.radioplayer.core.player.Playback
import net.afanasev.radioplayer.core.player.PlayerButtonState

/**
 * Same layout as the [PlayerButtonState] overload, but the play button follows [playback]:
 * it stays clickable while loading, so a tap cancels a connect or reconnect.
 */
@Composable
fun PortraitContent(
    title: String,
    nextTrackTitle: String,
    playback: Playback,
    artworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
    nextTrackPrefix: String = "Next",
) {
    PortraitContentLayout(title, nextTrackTitle, artworkUri, modifier, logo, nextTrackPrefix) {
        PlayButton(playback = playback, onClick = onPlayClick)
    }
}

@Composable
fun PortraitContent(
    title: String,
    nextTrackTitle: String,
    buttonState: PlayerButtonState,
    artworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
    nextTrackPrefix: String = "Next",
) {
    PortraitContentLayout(title, nextTrackTitle, artworkUri, modifier, logo, nextTrackPrefix) {
        PlayButton(buttonState = buttonState, onClick = onPlayClick)
    }
}

@Composable
private fun PortraitContentLayout(
    title: String,
    nextTrackTitle: String,
    artworkUri: String,
    modifier: Modifier,
    logo: @Composable () -> Unit,
    nextTrackPrefix: String,
    playButton: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        logo()
        Spacer(modifier = Modifier.height(24.dp))
        Artwork(
            artworkUri = artworkUri,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Title(
            text = title,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        NextTrack(
            text = nextTrackTitle,
            modifier = Modifier.fillMaxWidth(0.9f),
            prefix = nextTrackPrefix,
        )
        Spacer(modifier = Modifier.height(32.dp))
        playButton()
    }
}

@Preview(showBackground = true)
@Composable
private fun PortraitContentPreview() {
    PortraitContent(
        title = "Massive Attack — Teardrop",
        nextTrackTitle = "Next: Portishead — Glory Box",
        buttonState = PlayerButtonState.PLAYING,
        artworkUri = "",
        onPlayClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PortraitContentPreviewLoading() {
    PortraitContent(
        title = "",
        nextTrackTitle = "",
        playback = Playback.Connecting,
        artworkUri = "",
        onPlayClick = {},
    )
}
