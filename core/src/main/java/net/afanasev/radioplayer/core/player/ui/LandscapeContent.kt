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
import net.afanasev.radioplayer.core.player.NowPlaying
import net.afanasev.radioplayer.core.player.Playback
import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.player.PlayerState

/**
 * Player layout driven by [state]. A null [NowPlaying.artworkUri] falls back to
 * [defaultArtworkUri]; [statusText] is passed to [PlaybackStatus], shown under the button.
 */
@Composable
fun LandscapeContent(
    state: PlayerState,
    defaultArtworkUri: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit = {},
    nextTrackPrefix: String = "Next",
    statusText: (Playback) -> String? = ::defaultPlaybackStatusText,
) {
    LandscapeContentLayout(
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
    LandscapeContentLayout(
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
private fun LandscapeContentLayout(
    title: String,
    nextTrackTitle: String,
    artworkUri: String,
    modifier: Modifier,
    logo: @Composable () -> Unit,
    nextTrackPrefix: String,
    playButton: @Composable () -> Unit,
    status: @Composable () -> Unit,
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
            playButton()
            status()
        }
    }
}

@Preview(showBackground = true, widthDp = 720, heightDp = 360)
@Composable
private fun LandscapeContentPreview() {
    LandscapeContent(
        state = PlayerState(
            playback = Playback.Playing,
            nowPlaying = NowPlaying(title = "Massive Attack — Teardrop", nextTrackTitle = "Portishead — Glory Box"),
        ),
        defaultArtworkUri = "",
        onPlayClick = {},
    )
}

@Preview(showBackground = true, widthDp = 720, heightDp = 360)
@Composable
private fun LandscapeContentPreviewLoading() {
    LandscapeContent(
        state = PlayerState(playback = Playback.Reconnecting(attempt = 1)),
        defaultArtworkUri = "",
        onPlayClick = {},
    )
}
