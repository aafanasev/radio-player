package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.afanasev.radioplayer.core.player.PlayerButtonState

@Composable
fun PlayButton(
    buttonState: PlayerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    disabledContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
) {
    IconButton(
        enabled = buttonState != PlayerButtonState.LOADING,
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
        if (buttonState == PlayerButtonState.LOADING) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(54.dp),
            )
        } else {
            val icon: ImageVector
            val desc: String
            when (buttonState) {
                PlayerButtonState.PAUSED -> {
                    icon = Icons.Filled.PlayArrow
                    desc = "Play"
                }

                PlayerButtonState.PLAYING -> {
                    icon = Icons.Filled.Pause
                    desc = "Pause"
                }

                PlayerButtonState.LOADING -> throw IllegalStateException()
            }

            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = Color.White,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PlayButtonLoadingPreview() {
    PlayButton(buttonState = PlayerButtonState.LOADING, onClick = {})
}

@Preview
@Composable
private fun PlayButtonPlayingPreview() {
    PlayButton(buttonState = PlayerButtonState.PLAYING, onClick = {})
}

@Preview
@Composable
private fun PlayButtonPausedPreview() {
    PlayButton(buttonState = PlayerButtonState.PAUSED, onClick = {})
}
