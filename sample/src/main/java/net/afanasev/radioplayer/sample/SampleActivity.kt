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
    val title by viewModel.title.collectAsState()
    val nextTrackTitle by viewModel.nextTrackTitle.collectAsState()
    val buttonState by viewModel.buttonState.collectAsState()
    val artworkUri by viewModel.artworkUri.collectAsState()

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(
            title = title,
            nextTrackTitle = nextTrackTitle,
            buttonState = buttonState,
            artworkUri = artworkUri,
            onPlayClick = viewModel::playPause,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        PortraitContent(
            title = title,
            nextTrackTitle = nextTrackTitle,
            buttonState = buttonState,
            artworkUri = artworkUri,
            onPlayClick = viewModel::playPause,
            modifier = modifier.fillMaxSize(),
        )
    }
}
