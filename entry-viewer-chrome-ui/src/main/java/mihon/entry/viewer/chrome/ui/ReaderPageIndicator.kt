package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.OutlinedMediaText

@Composable
fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    ReaderProgressIndicator(
        text = "$currentPage / $totalPages",
        modifier = modifier,
    )
}

/** Progress text drawn straight over pages, outlined so it reads on any page. */
@Composable
fun ReaderProgressIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        OutlinedMediaText(text = text)
    }
}
