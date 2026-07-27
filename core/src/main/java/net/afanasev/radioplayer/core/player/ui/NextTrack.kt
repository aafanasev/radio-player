package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/** [prefix] is plain text (not an Android string resource) so callers can localize it however they like. */
@Composable
fun NextTrack(
    text: String,
    modifier: Modifier = Modifier,
    prefix: String = "Next",
) {
    Text(
        text = if (text.isEmpty()) "" else "$prefix: $text",
        textAlign = TextAlign.Center,
        minLines = 1,
        maxLines = 1,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.basicMarquee(iterations = Int.MAX_VALUE),
    )
}

@Preview
@Composable
private fun NextTrackPreview() {
    NextTrack(text = "Michael Jackson - Billie Jean")
}
