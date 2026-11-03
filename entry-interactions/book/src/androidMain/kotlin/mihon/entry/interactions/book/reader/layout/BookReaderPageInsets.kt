package mihon.entry.interactions.book.reader.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * The window edges the book's page keeps clear of. They are the system bars' even while hidden: the reader's controls
 * show and hide the bars with them, and a page that followed would change height on every tap, paginating the chapter
 * again and moving the reader to another page number.
 */
internal val bookReaderPageInsets: WindowInsets
    @Composable get() = WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
