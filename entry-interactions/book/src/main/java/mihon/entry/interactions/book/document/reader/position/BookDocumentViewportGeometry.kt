package mihon.entry.interactions.book.document.reader.position

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentTextPresentation
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import tachiyomi.domain.entry.model.EntryChapter

/** Actual visible text lines, independent of the center-based reading-progress estimator. */
internal class BookDocumentViewportGeometry {
    var viewport by mutableStateOf<LayoutCoordinates?>(null)
    val texts = mutableStateMapOf<String, BookDocumentViewportText>()

    fun firstLocation(items: List<BookDocumentViewerItem<EntryChapter>>): BookDocumentViewerLocation<EntryChapter>? {
        val viewport = viewport?.takeIf { it.isAttached } ?: return null
        val first = texts.values.mapNotNull { text ->
            val belongsToVisibleBlock = items.any { item ->
                item is BookDocumentViewerItem.Block && item.section.key == text.sectionKey &&
                    text.logicalStart in item.content.logicalStart until item.content.logicalEndExclusive
            }
            if (!belongsToVisibleBlock) return@mapNotNull null
            val top = text.topIn(viewport) ?: return@mapNotNull null
            val layout = text.layout ?: return@mapNotNull null
            if (top + layout.size.height <= 0 || top >= viewport.size.height) return@mapNotNull null
            Triple(text, top, layout)
        }.minByOrNull { it.second } ?: return null
        val (text, top, layout) = first
        val section = items.asSequence().filterIsInstance<BookDocumentViewerItem.Block<EntryChapter>>()
            .firstOrNull { it.section.key == text.sectionKey }?.section ?: return null
        val line = layout.getLineForVerticalPosition((-top).coerceAtLeast(0f))
        val displayOffset = layout.getLineStart(line)
        val sourceOffset = displayOffset - text.presentation.insertedOffsets.count { it < displayOffset }
        val document = section.document.document
        val position = document.positionAtLogicalOffset(text.logicalStart + sourceOffset)
        return BookDocumentViewerLocation(section, position, document.progressionAt(position))
    }

    fun lineTop(section: BookDocumentSection<EntryChapter>, position: BookDocumentPosition): Float? {
        val viewport = viewport?.takeIf { it.isAttached } ?: return null
        val absolute = section.document.document.logicalOffset(position) ?: return null
        return texts.values.firstNotNullOfOrNull { text ->
            if (text.sectionKey != section.key) return@firstNotNullOfOrNull null
            val layout = text.layout ?: return@firstNotNullOfOrNull null
            val length = text.presentation.text.length - text.presentation.insertedOffsets.size
            val offset = absolute - text.logicalStart
            if (offset !in 0..length) return@firstNotNullOfOrNull null
            val top = text.topIn(viewport) ?: return@firstNotNullOfOrNull null
            top + layout.getLineTop(layout.getLineForOffset(text.presentation.start(offset)))
        }
    }
}

internal class BookDocumentViewportText(
    val sectionKey: String,
    val logicalStart: Int,
    val presentation: BookDocumentTextPresentation,
) {
    var layout by mutableStateOf<TextLayoutResult?>(null)
    private var coordinates: LayoutCoordinates? = null
    private var placementRevision by mutableIntStateOf(0)

    fun placed(value: LayoutCoordinates) {
        coordinates = value
        placementRevision++
    }

    fun topIn(viewport: LayoutCoordinates): Float? {
        // A scroll changes the transform without replacing LayoutCoordinates. Observe completed
        // placement so a restore cannot publish geometry from the frame before scrollToItem.
        placementRevision
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return null
        return viewport.localPositionOf(coordinates, Offset.Zero).y
    }
}

internal val LocalBookDocumentViewportGeometry = staticCompositionLocalOf<BookDocumentViewportGeometry?> { null }
