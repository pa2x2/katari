package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPage
import tachiyomi.domain.entry.model.EntryChapter

/** Keeps live viewport observations cheap while publishing seek controls only when chrome is visible. */
@Stable
internal class BookDocumentSeekState {
    private var location: BookDocumentViewerLocation<EntryChapter>? = null
    private var pages: List<BookDocumentPage> = emptyList()
    private var visible = false
    var snapshot by mutableStateOf<BookDocumentSeekSnapshot?>(null)
        private set

    var pendingValue by mutableStateOf<Float?>(null)
        private set

    fun beginSeek(value: Float) {
        pendingValue = value
    }

    fun cancelSeek() {
        pendingValue = null
    }

    fun observe(location: BookDocumentViewerLocation<EntryChapter>) {
        this.location = location
        if (location.restoredNavigationId != null) pendingValue = null
        if (visible) {
            publish()
        } else if (snapshot?.section?.owner?.id != location.section.owner.id) {
            snapshot = null
        }
    }

    fun updatePages(pages: List<BookDocumentPage>) {
        if (this.pages === pages) return
        this.pages = pages
        if (visible) publish()
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
        // Preserve the rendered controls through exit/entry. Clearing here changes the bottom
        // bar height during its animation and forces a second composition on the next entrance.
        if (visible) publish()
    }

    private fun publish() {
        val location = location ?: return
        val previous = snapshot
        val positions = if (previous?.section === location.section && previous.pages === pages) {
            previous.pagePositions
        } else {
            pages.mapNotNull { page ->
                page.fragments.first().takeIf { it.section?.key == location.section.key }?.position
            }
        }
        if (previous?.section?.key != location.section.key || previous.pagePositions != positions) pendingValue = null
        snapshot = BookDocumentSeekSnapshot(
            location.section,
            location.position,
            positions,
            pages,
            location.viewportEndProgression,
        )
    }
}
