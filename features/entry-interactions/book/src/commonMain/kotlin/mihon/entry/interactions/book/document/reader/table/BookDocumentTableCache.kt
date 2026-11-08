package mihon.entry.interactions.book.document.reader.table

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import mihon.book.api.document.BookDocumentBlock
import java.util.IdentityHashMap

internal val LocalBookDocumentTableCache = compositionLocalOf<BookDocumentTableCache?> { null }

/** Retains only geometry for the loaded reading window and its current display configuration. */
internal class BookDocumentTableCache {
    val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val entries = IdentityHashMap<BookDocumentBlock, Entry>()

    @Synchronized
    fun readingWidth(block: BookDocumentBlock): Int? = entries[block]?.width

    @Synchronized
    fun get(block: BookDocumentBlock, width: Int, fonts: Map<String, FontFamily>): BookDocumentTableGeometry? =
        entries[block]?.takeIf { it.width == width && it.fonts == fonts }?.geometry

    @Synchronized
    fun put(block: BookDocumentBlock, width: Int, fonts: Map<String, FontFamily>, geometry: BookDocumentTableGeometry) {
        entries[block] = Entry(width, fonts, geometry)
    }

    @Synchronized
    fun retain(blocks: List<BookDocumentBlock>) {
        val retained = IdentityHashMap<BookDocumentBlock, Boolean>()
        blocks.forEach { retained[it] = true }
        entries.keys.retainAll(retained.keys)
    }

    private class Entry(val width: Int, val fonts: Map<String, FontFamily>, val geometry: BookDocumentTableGeometry)
}
