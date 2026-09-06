package mihon.entry.interactions.reader.settings

import androidx.annotation.DrawableRes
import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.api.R
import tachiyomi.i18n.MR

/** BOOK layout preference; persisted by name independently of document locators. */
enum class BookDocumentReadingMode(val stringRes: StringResource, @DrawableRes val iconRes: Int) {
    SCROLL(MR.strings.book_reader_continuous_scroll, R.drawable.ic_reader_continuous_vertical_24dp),
    PAGED_LTR(MR.strings.left_to_right_viewer, R.drawable.ic_reader_ltr_24dp),
    PAGED_RTL(MR.strings.right_to_left_viewer, R.drawable.ic_reader_rtl_24dp),
    PAGED_VERTICAL(MR.strings.vertical_viewer, R.drawable.ic_reader_vertical_24dp),
}
