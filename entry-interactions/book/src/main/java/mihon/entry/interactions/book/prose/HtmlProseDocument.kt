package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.text.HtmlCompat
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
) {
    val progression: Float
        get() = if (total <= 1) 1f else index.toFloat() / (total - 1)
}

internal fun parseProseHtml(bodyHtml: String, paragraphSpacingPercent: Int = 100): Spanned {
    val parsed = SpannableStringBuilder(
        HtmlCompat.fromHtml(bodyHtml, HtmlCompat.FROM_HTML_MODE_LEGACY),
    )
    val replacement = "\n".repeat(1 + (paragraphSpacingPercent.coerceIn(0, 200) / 100))
    var index = parsed.length - 1
    while (index >= 0) {
        if (parsed[index] == '\n') {
            val end = index + 1
            while (index >= 0 && parsed[index] == '\n') index--
            val start = index + 1
            if (end - start >= 2) parsed.replace(start, end, replacement)
        } else {
            index--
        }
    }
    return parsed
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
            val lastLine = layout.getLineForVertical(pageBottom - 1)
                .coerceIn(firstLine, layout.lineCount - 1)
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
        )
    }
}
