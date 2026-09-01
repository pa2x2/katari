package mihon.entry.interactions.book.document.reader

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset

/** Reader projection of one visible text leaf and the text that follows it in reading order. */
internal data class BookDocumentSelectableLeaf(
    val token: String,
    val chapterId: Long,
    val fullText: String,
    val leadingText: String = "",
    val separatorAfter: String,
)

internal data class BookDocumentSelectionLayout(
    val textLayout: TextLayoutResult? = null,
    val positionInWindow: IntOffset? = null,
)

internal data class BookDocumentSelectionProjection(
    val chapterId: Long,
    val selectedTokens: Set<String>,
    val identity: String,
    val text: String,
    val languageContextText: String,
    val boundsInReaderRoot: RectF?,
)

internal fun projectBookDocumentSelection(
    ownerIdentity: String,
    selectedTexts: List<AnnotatedString>,
    selectableLeaves: Map<String, BookDocumentSelectableLeaf>,
    layouts: Map<String, BookDocumentSelectionLayout>,
    readerRootPositionInWindow: Offset,
): BookDocumentSelectionProjection? {
    val fragments = selectedTexts.mapNotNull { selectedText ->
        val token = selectedText
            .getStringAnnotations(
                tag = BOOK_DOCUMENT_SELECTION_TOKEN_TAG,
                start = 0,
                end = selectedText.length,
            )
            .firstOrNull()
            ?.item
            ?: return@mapNotNull null
        val leaf = selectableLeaves[token] ?: return@mapNotNull null
        BookDocumentSelectionFragment(leaf, selectedText.text)
    }
    if (fragments.isEmpty()) return null
    val chapterId = fragments.first().metadata.chapterId
    if (fragments.any { it.metadata.chapterId != chapterId }) return null

    val resolvedFragments = fragments.mapIndexed { index, fragment ->
        fragment.resolveRange(index, fragments.lastIndex)
    }
    val text = buildString {
        resolvedFragments.forEachIndexed { index, fragment ->
            if (index > 0) append(resolvedFragments[index - 1].fragment.metadata.separatorAfter)
            if (fragment.start == 0) append(fragment.fragment.metadata.leadingText)
            append(fragment.fragment.text)
        }
    }
    if (text.isBlank()) return null
    val identity = buildString {
        append(ownerIdentity)
        resolvedFragments.forEach { fragment ->
            append('|')
            append(fragment.fragment.metadata.token)
            append(':')
            append(fragment.start)
            append(':')
            append(fragment.endExclusive)
        }
    }
    val languageContextText = resolvedFragments
        .map(ResolvedBookDocumentSelectionFragment::languageContextText)
        .distinct()
        .joinToString(separator = "\n")
        .takeCodePoints(MAX_LANGUAGE_CONTEXT_CODE_POINTS)
    val bounds = resolvedFragments
        .mapNotNull { fragment ->
            fragment.boundsInReaderRoot(
                layout = layouts[fragment.fragment.metadata.token],
                readerRootPositionInWindow = readerRootPositionInWindow,
            )
        }
        .reduceOrNull(RectF::unionWith)

    return BookDocumentSelectionProjection(
        chapterId = chapterId,
        selectedTokens = fragments.mapTo(linkedSetOf()) { it.metadata.token },
        identity = identity,
        text = text,
        languageContextText = languageContextText,
        boundsInReaderRoot = bounds,
    )
}

private data class BookDocumentSelectionFragment(
    val metadata: BookDocumentSelectableLeaf,
    val text: String,
) {
    fun resolveRange(index: Int, lastIndex: Int): ResolvedBookDocumentSelectionFragment {
        val fullText = metadata.fullText
        val start = when {
            text == fullText -> 0
            index == 0 && lastIndex > 0 -> fullText.lastIndexOf(text)
            else -> fullText.indexOf(text)
        }.coerceAtLeast(0)
        return ResolvedBookDocumentSelectionFragment(
            fragment = this,
            start = start,
            endExclusive = (start + text.length).coerceAtMost(fullText.length),
        )
    }
}

private data class ResolvedBookDocumentSelectionFragment(
    val fragment: BookDocumentSelectionFragment,
    val start: Int,
    val endExclusive: Int,
) {
    fun languageContextText(): String {
        val fullText = fragment.metadata.fullText
        val fullCodePoints = fullText.codePointCount(0, fullText.length)
        if (fullCodePoints <= MAX_LANGUAGE_CONTEXT_CODE_POINTS) return fullText

        val selectionStart = fullText.codePointCount(0, start)
        val selectionEnd = fullText.codePointCount(0, endExclusive)
        val selectionLength = selectionEnd - selectionStart
        if (selectionLength >= MAX_LANGUAGE_CONTEXT_CODE_POINTS) {
            return fragment.text.takeCodePoints(MAX_LANGUAGE_CONTEXT_CODE_POINTS)
        }

        val surroundingCapacity = MAX_LANGUAGE_CONTEXT_CODE_POINTS - selectionLength
        var windowStart = (selectionStart - surroundingCapacity / 2).coerceAtLeast(0)
        var windowEnd = (windowStart + MAX_LANGUAGE_CONTEXT_CODE_POINTS).coerceAtMost(fullCodePoints)
        windowStart = (windowEnd - MAX_LANGUAGE_CONTEXT_CODE_POINTS).coerceAtLeast(0)
        val startOffset = fullText.offsetByCodePoints(0, windowStart)
        val endOffset = fullText.offsetByCodePoints(0, windowEnd)
        return fullText.substring(startOffset, endOffset)
    }

    fun boundsInReaderRoot(
        layout: BookDocumentSelectionLayout?,
        readerRootPositionInWindow: Offset,
    ): RectF? {
        val textLayout = layout?.textLayout ?: return null
        val positionInWindow = layout.positionInWindow ?: return null
        if (endExclusive <= start) return null
        val localBounds = textLayout.getPathForRange(start, endExclusive).getBounds()
        if (localBounds.isEmpty) return null
        val left = localBounds.left + positionInWindow.x - readerRootPositionInWindow.x
        val top = localBounds.top + positionInWindow.y - readerRootPositionInWindow.y
        return RectF(
            left,
            top,
            localBounds.right + positionInWindow.x - readerRootPositionInWindow.x,
            localBounds.bottom + positionInWindow.y - readerRootPositionInWindow.y,
        )
    }
}

private fun RectF.unionWith(other: RectF): RectF = RectF(this).apply { union(other) }

private fun String.takeCodePoints(maximum: Int): String {
    val count = codePointCount(0, length)
    if (count <= maximum) return this
    return substring(0, offsetByCodePoints(0, maximum))
}

internal const val BOOK_DOCUMENT_SELECTION_TOKEN_TAG = "book-document-selection-token"
private const val MAX_LANGUAGE_CONTEXT_CODE_POINTS = 1_000
