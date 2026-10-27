package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/** How many pages the paged left-to-right and right-to-left modes show at once. */
enum class ReaderPageLayout(val stringRes: StringResource) {
    /** Two pages side by side where the window shows two panes, split at a fold; one page elsewhere. */
    AUTOMATIC(MR.strings.reader_pages_automatic),
    ONE_PAGE(MR.strings.reader_pages_one),
}
