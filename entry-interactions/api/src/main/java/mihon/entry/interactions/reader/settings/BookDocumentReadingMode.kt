package mihon.entry.interactions.reader.settings

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.ReaderLeftToRight
import mihon.icons.custommaterialsymbols.rounded.ReaderLongStripGaps
import mihon.icons.custommaterialsymbols.rounded.ReaderRightToLeft
import mihon.icons.custommaterialsymbols.rounded.ReaderVertical
import tachiyomi.i18n.*

/** BOOK layout preference; persisted by name independently of document locators. */
enum class BookDocumentReadingMode(val stringRes: StringResource, val icon: ImageVector) {
    SCROLL(MR.strings.book_reader_continuous_scroll, CustomMaterialSymbols.Rounded.ReaderLongStripGaps),
    PAGED_LTR(MR.strings.left_to_right_viewer, CustomMaterialSymbols.Rounded.ReaderLeftToRight),
    PAGED_RTL(MR.strings.right_to_left_viewer, CustomMaterialSymbols.Rounded.ReaderRightToLeft),
    PAGED_VERTICAL(MR.strings.vertical_viewer, CustomMaterialSymbols.Rounded.ReaderVertical),
    ;

    /** Pages turn sideways, so two can lie side by side like an open book. */
    val isHorizontallyPaged: Boolean get() = this == PAGED_LTR || this == PAGED_RTL
}
