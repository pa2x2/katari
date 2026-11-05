package mihon.entry.interactions.book.reader.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.book.reader.BookReaderProgressInsets

/** A desktop window's corners never cut into the reader. */
@Composable
internal actual fun BookReaderRoundedCornerSafeArea(
    modifier: Modifier,
    content: @Composable BoxScope.(BookReaderProgressInsets) -> Unit,
) {
    Box(modifier = modifier, content = { content(BookReaderProgressInsets.Zero) })
}
