package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.text.HtmlCompat
import org.jsoup.Jsoup
import tachiyomi.domain.entry.model.EntryChapter

internal data class HtmlProseLoadedChapter(
    val chapter: EntryChapter,
    val resourceId: String,
    val bodyHtml: String,
    val initialProgression: Float,
)

internal data class HtmlProsePage(
    val chapter: EntryChapter,
    val index: Int,
    val total: Int,
    val text: CharSequence,
    val sourceStart: Int = 0,
    val sourceEndExclusive: Int = sourceStart + text.length,
) {
    val progression: Float
        get() = if (total <= 1) 1f else index.toFloat() / (total - 1)
}

internal data class ParsedProseDocument(
    val text: Spanned,
    val anchors: Map<String, Int>,
)

internal fun parseProseHtml(bodyHtml: String): ParsedProseDocument {
    val document = Jsoup.parseBodyFragment(bodyHtml)
    val anchorMarkers = buildList {
        document.select("[id], a[name]").forEach { element ->
            val ids = listOf(element.id(), element.attr("name")).filter(String::isNotBlank).distinct()
            if (ids.isEmpty()) return@forEach
            val marker = "$ANCHOR_MARKER_START${size.toString(36)}$ANCHOR_MARKER_END"
            element.prependText(marker)
            add(marker to ids)
        }
    }
    val parsed = SpannableStringBuilder(
        HtmlCompat.fromHtml(document.body().html(), HtmlCompat.FROM_HTML_MODE_LEGACY),
    )
    var index = parsed.length - 1
    while (index >= 0) {
        if (parsed[index] == '\n') {
            val end = index + 1
            while (index >= 0 && parsed[index] == '\n') index--
            val start = index + 1
            if (end - start >= 2) parsed.replace(start, end, "\n\n")
        } else {
            index--
        }
    }
    val anchors = buildMap {
        anchorMarkers.forEach { (marker, ids) ->
            val markerIndex = parsed.indexOf(marker)
            if (markerIndex < 0) return@forEach
            ids.forEach { id -> putIfAbsent(id, markerIndex) }
            parsed.delete(markerIndex, markerIndex + marker.length)
        }
    }
    return ParsedProseDocument(parsed, anchors)
}

internal fun paginateProse(
    chapter: HtmlProseLoadedChapter,
    text: Spanned,
    paint: TextPaint,
    availableWidthPx: Int,
    availableHeightPx: Int,
    alignment: Layout.Alignment,
    lineSpacingMultiplier: Float,
    justificationMode: Int = Layout.JUSTIFICATION_MODE_NONE,
): List<HtmlProsePage> {
    if (text.isEmpty() || availableWidthPx <= 0 || availableHeightPx <= 0) return emptyList()
    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
        .setAlignment(alignment)
        .setIncludePad(false)
        .setLineSpacing(0f, lineSpacingMultiplier)
    if (justificationMode != Layout.JUSTIFICATION_MODE_NONE) builder.setJustificationMode(justificationMode)
    val layout = builder.build()
    if (layout.lineCount == 0) return emptyList()

    val ranges = buildList {
        var firstLine = 0
        while (firstLine < layout.lineCount) {
            val pageBottom = layout.getLineTop(firstLine) + availableHeightPx
            var lastLine = layout.getLineForVertical(pageBottom - 1)
                .coerceIn(firstLine, layout.lineCount - 1)
            while (lastLine > firstLine && layout.getLineBottom(lastLine) > pageBottom) {
                lastLine--
            }
            val start = layout.getLineStart(firstLine)
            val end = layout.getLineEnd(lastLine).coerceIn(start, text.length)
            if (end > start) add(start until end)
            firstLine = lastLine + 1
        }
    }
    return ranges.mapIndexed { index, range ->
        HtmlProsePage(
            chapter = chapter.chapter,
            index = index,
            total = ranges.size,
            text = text.subSequence(range.first, range.last + 1),
            sourceStart = range.first,
            sourceEndExclusive = range.last + 1,
        )
    }
}

internal fun pageIndexForAnchor(pages: List<HtmlProsePage>, anchorOffset: Int): Int? {
    return pages.indexOfFirst { page ->
        anchorOffset >= page.sourceStart &&
            (
                anchorOffset < page.sourceEndExclusive ||
                    (page.index == pages.lastIndex && anchorOffset == page.sourceEndExclusive)
                )
    }.takeIf { it >= 0 }
}

private const val ANCHOR_MARKER_START = '\uE000'
private const val ANCHOR_MARKER_END = '\uE001'
