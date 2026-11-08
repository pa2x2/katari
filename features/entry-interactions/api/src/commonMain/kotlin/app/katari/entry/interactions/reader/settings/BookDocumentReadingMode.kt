package app.katari.entry.interactions.reader.settings

import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.katari.icons.custommaterialsymbols.rounded.ReaderLeftToRight
import app.katari.icons.custommaterialsymbols.rounded.ReaderLongStripGaps
import app.katari.icons.custommaterialsymbols.rounded.ReaderRightToLeft
import app.katari.icons.custommaterialsymbols.rounded.ReaderVertical
import dev.icerock.moko.resources.StringResource
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
