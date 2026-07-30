package mihon.entry.interactions.book

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.book.api.BookReadingDirection
import mihon.entry.interactions.reader.settings.BookReaderLayoutMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import mihon.entry.interactions.api.R as EntryInteractionsR

@Composable
internal fun BookReaderLayoutButton(
    layoutMode: BookReaderLayoutMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    readingDirection: BookReadingDirection? = null,
) {
    val stateDescription = stringResource(layoutMode.labelRes)
    val iconRes = when (layoutMode) {
        BookReaderLayoutMode.PAGINATED -> {
            if (readingDirection == BookReadingDirection.RIGHT_TO_LEFT) {
                EntryInteractionsR.drawable.ic_reader_rtl_24dp
            } else {
                EntryInteractionsR.drawable.ic_reader_ltr_24dp
            }
        }
        BookReaderLayoutMode.SCROLLING -> EntryInteractionsR.drawable.ic_reader_webtoon_24dp
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            this.stateDescription = stateDescription
        },
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(MR.strings.book_reader_layout),
        )
    }
}
