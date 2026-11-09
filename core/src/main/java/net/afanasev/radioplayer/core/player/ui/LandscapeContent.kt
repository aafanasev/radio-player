package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.afanasev.radioplayer.core.player.PlayerButtonState

@Composable
fun LandscapeContent(
    title: String,
    nextTrackTitle: String,
    buttonState: PlayerButtonState,
    artworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
    nextTrackPrefix: String = "Next",
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(
                artworkUri = artworkUri,
                modifier = Modifier.fillMaxHeight(0.8f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            logo()
            Spacer(modifier = Modifier.height(18.dp))
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
            Spacer(modifier = Modifier.height(24.dp))
            PlayButton(
                buttonState = buttonState,
                onClick = onPlayClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 720, heightDp = 360)
@Composable
private fun LandscapeContentPreview() {
    LandscapeContent(
        title = "Massive Attack — Teardrop",
        nextTrackTitle = "Next: Portishead — Glory Box",
        buttonState = PlayerButtonState.PLAYING,
        artworkUri = "",
        onPlayClick = {},
    )
}

@Preview(showBackground = true, widthDp = 720, heightDp = 360)
@Composable
private fun LandscapeContentPreviewLoading() {
    LandscapeContent(
        title = "",
        nextTrackTitle = "",
        buttonState = PlayerButtonState.LOADING,
        artworkUri = "",
        onPlayClick = {},
    )
}
