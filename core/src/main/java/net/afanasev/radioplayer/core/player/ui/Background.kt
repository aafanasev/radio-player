package net.afanasev.radioplayer.core.player.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlin.random.Random

private val DefaultGradients: List<Pair<Color, Color>> = listOf(
    Color(0xFF1F263F) to Color(0xFF394267),
    Color(0xFF264B99) to Color(0xFF592380),
)

/**
 * Animates between a set of gradients while [artworkUri] is [defaultArtworkUri] (nothing playing
 * yet), then crossfades to a blurred artwork background once real artwork is available.
 * [gradients] defaults to two neutral gradients; pass your own for on-brand placeholder colors.
 */
@Composable
fun Background(
    artworkUri: String,
    defaultArtworkUri: String,
    modifier: Modifier = Modifier,
    gradients: List<Pair<Color, Color>> = DefaultGradients,
) {
    Crossfade(
        targetState = artworkUri == defaultArtworkUri,
        animationSpec = tween(1_000),
    ) { showGradient ->
        if (showGradient) {
            val size = gradients.size
            var current by remember { mutableIntStateOf(Random.nextInt(size)) }
            val topColor = remember { Animatable(gradients[current].first) }
            val bottomColor = remember { Animatable(gradients[current].second) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(10_000)
                    current = (current + 1) % size
                }
            }

            LaunchedEffect(current) {
                val next = gradients[current]
                topColor.animateTo(next.first, animationSpec = tween(durationMillis = 3_000))
                bottomColor.animateTo(next.second, animationSpec = tween(durationMillis = 3_000))
            }

            Box(
                modifier = modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topColor.value,
                            bottomColor.value,
                        )
                    )
                ),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    Color.Black.copy(alpha = 0.3f),
                    BlendMode.Darken,
                ),
                modifier = modifier.blur(20.dp),
            )
        }
    }
}
