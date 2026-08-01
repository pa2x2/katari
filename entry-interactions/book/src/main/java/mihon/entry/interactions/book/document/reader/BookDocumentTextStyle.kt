package mihon.entry.interactions.book.document.reader

import android.graphics.Typeface

internal data class BookDocumentTextStyle(
    val textColor: Int,
    val linkTextColor: Int,
    val textSizeSp: Float,
    val typeface: Typeface,
    val lineSpacingMultiplier: Float,
    val textAlignment: Int,
    val justificationMode: Int,
)

internal fun BookDocumentTextView.applyStyle(style: BookDocumentTextStyle) {
    if (appliedStyle == style) return
    setTextColor(style.textColor)
    setLinkTextColor(style.linkTextColor)
    textSize = style.textSizeSp
    typeface = style.typeface
    setLineSpacing(0f, style.lineSpacingMultiplier)
    textAlignment = style.textAlignment
    justificationMode = style.justificationMode
    appliedStyle = style
}
