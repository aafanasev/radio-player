package net.afanasev.radioplayer.sample

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.afanasev.radioplayer.core.player.PlayerViewModel
import net.afanasev.radioplayer.core.player.toButtonState
import net.afanasev.radioplayer.core.player.ui.LandscapeContent
import net.afanasev.radioplayer.core.player.ui.PortraitContent

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerScreen(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(modifier: Modifier = Modifier) {
    val viewModel: PlayerViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val nowPlaying = state.nowPlaying
    val artworkUri = nowPlaying.artworkUri ?: viewModel.defaultArtworkUri

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(
            title = nowPlaying.title,
            nextTrackTitle = nowPlaying.nextTrackTitle,
            buttonState = state.playback.toButtonState(),
            artworkUri = artworkUri,
            onPlayClick = viewModel::onPlayPause,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        PortraitContent(
            title = nowPlaying.title,
            nextTrackTitle = nowPlaying.nextTrackTitle,
            buttonState = state.playback.toButtonState(),
            artworkUri = artworkUri,
            onPlayClick = viewModel::onPlayPause,
            modifier = modifier.fillMaxSize(),
        )
    }
}
