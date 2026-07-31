package mihon.entry.interactions.book.document.reader

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun BookDocumentText(
    text: Spanned,
    documentTextIdentity: String,
    textColor: Int,
    textSizeSp: Float,
    typeface: Typeface,
    lineSpacingMultiplier: Float,
    textAlignment: Int,
    justificationMode: Int,
    trimTerminalLine: Boolean = false,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit = {},
    onNonLinkClick: (() -> Unit)? = null,
    onViewChanged: (TextView?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = LocalBookDocumentTextInteraction.current
    val documentSectionKey = LocalBookDocumentSectionKey.current
    val currentAnchorClick by rememberUpdatedState(onAnchorClick)
    val currentExternalLinkClick by rememberUpdatedState(onExternalLinkClick)
    val currentNonLinkClick by rememberUpdatedState(onNonLinkClick)
    val linkedText = remember(text, trimTerminalLine) {
        val displayText = if (trimTerminalLine) {
            text.withoutTerminalLayoutLine()
        } else {
            text
        }
        displayText.withDocumentLinkClicks(
            onAnchorClick = { anchorId, view -> currentAnchorClick(anchorId, view) },
            onExternalLinkClick = { url -> currentExternalLinkClick(url) },
        )
    }
    val style = BookDocumentTextStyle(
        textColor = textColor,
        textSizeSp = textSizeSp,
        typeface = typeface,
        lineSpacingMultiplier = lineSpacingMultiplier,
        textAlignment = textAlignment,
        justificationMode = justificationMode,
    )
    var textView by remember { mutableStateOf<BookDocumentTextView?>(null) }
    AndroidView(
        modifier = modifier.onLayoutRectChanged(
            throttleMillis = SELECTION_ANCHOR_REFRESH_INTERVAL_MILLIS,
            debounceMillis = 0,
        ) {
            textView?.refreshOwnedSelectionAnchor()
        },
        factory = { context ->
            BookDocumentTextView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                movementMethod = LinkMovementMethod.getInstance()
                applyVisibleSelectionHighlight()
                this.documentSectionKey = documentSectionKey
                onDocumentAnchorClick = currentAnchorClick
                onDocumentExternalLinkClick = currentExternalLinkClick
                onDocumentNonLinkClick = currentNonLinkClick
                textView = this
                onViewChanged(this)
            }
        },
        update = { view ->
            view.selectionInteraction = interaction
            view.documentSectionKey = documentSectionKey
            view.onDocumentAnchorClick = currentAnchorClick
            view.onDocumentExternalLinkClick = currentExternalLinkClick
            view.onDocumentNonLinkClick = currentNonLinkClick
            view.updateDocumentText(
                identity = "$documentSectionKey:$documentTextIdentity",
                linkedText = linkedText,
            )
            view.movementMethod = LinkMovementMethod.getInstance()
            view.applyStyle(style)
            view.applyTerminalLineSpacing(trimTerminalLine)
            onViewChanged(view)
        },
        onRelease = { view ->
            view.clearOwnedSelection()
            view.documentSectionKey = null
            view.appliedDocumentTextIdentity = null
            view.onDocumentAnchorClick = null
            view.onDocumentExternalLinkClick = null
            view.onDocumentNonLinkClick = null
            if (textView === view) textView = null
            onViewChanged(null)
            view.text = null
        },
    )
}

private const val SELECTION_ANCHOR_REFRESH_INTERVAL_MILLIS = 16L
