package mihon.entry.interactions.book.document.reader

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.toBookDocumentLinkTarget

internal fun Spanned.withoutTerminalLayoutLine(): Spanned {
    val displayEnd = indexOfLast { it != '\n' } + 1
    if (displayEnd == length) return this
    return SpannableString(subSequence(0, displayEnd))
}

internal fun Spanned.withDocumentAnchorClicks(
    onAnchorClick: (String, TextView) -> Unit,
): Spanned = withDocumentLinkClicks(onAnchorClick, onExternalLinkClick = {})

internal fun Spanned.withDocumentLinkClicks(
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
): Spanned {
    val spannable = SpannableString(this)
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach { span ->
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span)
        spannable.removeSpan(span)
        val target = span.url.toBookDocumentLinkTarget() ?: return@forEach
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if ((widget as? BookDocumentTextView)?.dispatchDocumentLink(target) == true) return
                    when (target) {
                        is BookDocumentLinkTarget.Anchor ->
                            (widget as? TextView)?.let { onAnchorClick(target.fragment, it) }
                        is BookDocumentLinkTarget.External -> onExternalLinkClick(target.url)
                    }
                }
            },
            start,
            end,
            flags.takeIf { it != 0 } ?: Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return spannable
}
