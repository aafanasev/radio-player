package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.afanasev.radioplayer.core.player.NowPlaying
import net.afanasev.radioplayer.core.player.Playback
import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.player.PlayerState

/**
 * Player layout driven by [state]. A null [NowPlaying.artworkUri] falls back to
 * [defaultArtworkUri]; [statusText] is passed to [PlaybackStatus], shown under the button.
 */
@Composable
fun PortraitContent(
    state: PlayerState,
    defaultArtworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
    nextTrackPrefix: String = "Next",
    statusText: (Playback) -> String? = ::defaultPlaybackStatusText,
) {
    PortraitContentLayout(
        title = state.nowPlaying.title,
        nextTrackTitle = state.nowPlaying.nextTrackTitle,
        artworkUri = state.nowPlaying.artworkUri ?: defaultArtworkUri,
        modifier = modifier,
        logo = logo,
        nextTrackPrefix = nextTrackPrefix,
        playButton = { PlayButton(playback = state.playback, onClick = onPlayClick) },
        status = {
            PlaybackStatus(
                playback = state.playback,
                text = statusText,
                modifier = Modifier.fillMaxWidth(0.9f).padding(top = 12.dp),
            )
        },
    )
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
    PortraitContentLayout(
        title = title,
        nextTrackTitle = nextTrackTitle,
        artworkUri = artworkUri,
        modifier = modifier,
        logo = logo,
        nextTrackPrefix = nextTrackPrefix,
        playButton = { PlayButton(buttonState = buttonState, onClick = onPlayClick) },
        status = {},
    )
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
    status: @Composable () -> Unit,
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
        status()
    }
}

@Preview(showBackground = true)
@Composable
private fun PortraitContentPreview() {
    PortraitContent(
        state = PlayerState(
            playback = Playback.Playing,
            nowPlaying = NowPlaying(title = "Massive Attack — Teardrop", nextTrackTitle = "Portishead — Glory Box"),
        ),
        defaultArtworkUri = "",
        onPlayClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PortraitContentPreviewLoading() {
    PortraitContent(
        state = PlayerState(playback = Playback.Reconnecting(attempt = 1)),
        defaultArtworkUri = "",
        onPlayClick = {},
    )
}
