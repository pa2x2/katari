package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextLayoutResult

/** Only installed in unplaced measurement slots; displayed text keeps its normal selection owner. */
internal class BookDocumentPageMeasurement {
    var lineEnds: List<Int> = emptyList()
        private set

    fun record(result: TextLayoutResult, insertedOffsets: Set<Int>) {
        lineEnds = (0 until result.lineCount).map { line ->
            val end = result.getLineEnd(line)
            end - insertedOffsets.count { it < end }
        }.filter { it > 0 }.distinct()
    }
}

internal val LocalBookDocumentPageMeasurement = staticCompositionLocalOf<BookDocumentPageMeasurement?> { null }
