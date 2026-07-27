package net.afanasev.radioplayer.core.player.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Title(
    text: String,
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            style = MaterialTheme.typography.titleLarge,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun TitlePreview() {
    Title(
        text = "Michael Jackson - Billie Jean",
        modifier = Modifier.width(100.dp),
    )
}
