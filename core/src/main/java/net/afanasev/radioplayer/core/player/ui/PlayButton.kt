package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.afanasev.radioplayer.core.player.Playback
import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.player.toButtonState

/**
 * Play button driven by [playback]. While loading it shows a spinner and stays clickable, so a
 * tap cancels; it's only disabled while no controller is connected yet.
 */
@Composable
fun PlayButton(
    playback: Playback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    disabledContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
) {
    PlayButton(
        buttonState = playback.toButtonState(),
        onClick = onClick,
        modifier = modifier,
        enabled = playback != Playback.Disconnected,
        containerColor = containerColor,
        disabledContainerColor = disabledContainerColor,
    )
}

/**
 * [enabled] defaults to true: a [PlayerButtonState.LOADING] button is clickable (to cancel)
 * and shows a stop mark inside the spinner.
 */
@Composable
fun PlayButton(
    buttonState: PlayerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    disabledContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor,
        ),
        modifier = modifier
            .size(96.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            ),
    ) {
        when (buttonState) {
            PlayerButtonState.LOADING -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(54.dp),
                )
                if (enabled) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            PlayerButtonState.PAUSED -> Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(54.dp),
            )

            PlayerButtonState.PLAYING -> Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = "Pause",
                tint = Color.White,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PlayButtonLoadingPreview() {
    PlayButton(playback = Playback.Connecting, onClick = {})
}

@Preview
@Composable
private fun PlayButtonDisconnectedPreview() {
    PlayButton(playback = Playback.Disconnected, onClick = {})
}

@Preview
@Composable
private fun PlayButtonPlayingPreview() {
    PlayButton(playback = Playback.Playing, onClick = {})
}

@Preview
@Composable
private fun PlayButtonPausedPreview() {
    PlayButton(playback = Playback.Idle, onClick = {})
}
