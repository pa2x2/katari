package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import tachiyomi.domain.entry.model.EntryChapter

/** One session-scoped return point, captured before the most recent explicit jump and retained through reflow. */
internal class BookDocumentJumpHistory {
    private var location: BookDocumentViewerLocation<EntryChapter>? = null
    var returnTarget by mutableStateOf<BookDocumentNavigationTarget?>(null)
        private set

    fun observe(location: BookDocumentViewerLocation<EntryChapter>) {
        this.location = location
    }

    fun rememberOrigin() {
        val location = location ?: return
        returnTarget = BookDocumentNavigationTarget(
            chapter = location.section.owner,
            locator = location.section.document.document.locatorAt(location.position),
            restorePosition = true,
        )
    }

    fun dismiss() {
        returnTarget = null
    }
}
