package mihon.entry.interactions.book.reader.layout

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.book.reader.BookReaderProgressInsets

/** Keeps edge-bound reader overlays within the rectangular safe area of the screen's rounded corners. */
@Composable
internal expect fun BookReaderRoundedCornerSafeArea(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(BookReaderProgressInsets) -> Unit,
)
