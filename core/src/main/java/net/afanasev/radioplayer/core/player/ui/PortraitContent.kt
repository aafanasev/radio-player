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
import net.afanasev.radioplayer.core.player.PlayerButtonState

@Composable
fun PortraitContent(
    title: String,
    nextTrackTitle: String,
    buttonState: PlayerButtonState,
    artworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
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
        )
        Spacer(modifier = Modifier.height(32.dp))
        PlayButton(
            buttonState = buttonState,
            onClick = onPlayClick,
        )
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
        buttonState = PlayerButtonState.LOADING,
        artworkUri = "",
        onPlayClick = {},
    )
}
