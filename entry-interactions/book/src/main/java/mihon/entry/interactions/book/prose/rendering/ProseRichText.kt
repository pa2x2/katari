package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Spanned
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import mihon.entry.interactions.book.document.reader.BookDocumentText

@Composable
internal fun ProseRichText(
    text: Spanned,
    documentTextIdentity: String,
    textColor: Int,
    textSizeSp: Float,
    typeface: Typeface,
    lineSpacingMultiplier: Float,
    textAlignment: Int,
    justificationMode: Int,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onNonLinkClick: (() -> Unit)? = null,
    anchorCharacterOffset: Int?,
    onAnchorTargetPositioned: (LayoutCoordinates, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textView by remember(text, anchorCharacterOffset) { mutableStateOf<TextView?>(null) }
    var coordinates by remember(text, anchorCharacterOffset) { mutableStateOf<LayoutCoordinates?>(null) }
    LaunchedEffect(textView, coordinates, anchorCharacterOffset, onAnchorTargetPositioned) {
        val target = anchorCharacterOffset ?: return@LaunchedEffect
        val view = textView ?: return@LaunchedEffect
        val positioned = coordinates?.takeIf(LayoutCoordinates::isAttached) ?: return@LaunchedEffect
        val layout = view.layout ?: return@LaunchedEffect
        val boundedOffset = target.coerceIn(0, view.text.length)
        val lineTop = layout.getLineTop(layout.getLineForOffset(boundedOffset))
        onAnchorTargetPositioned(positioned, lineTop)
    }
    BookDocumentText(
        text = text,
        documentTextIdentity = documentTextIdentity,
        modifier = modifier.then(
            if (anchorCharacterOffset != null) {
                Modifier.onGloballyPositioned { coordinates = it }
            } else {
                Modifier
            },
        ),
        textColor = textColor,
        textSizeSp = textSizeSp,
        typeface = typeface,
        lineSpacingMultiplier = lineSpacingMultiplier,
        textAlignment = textAlignment,
        justificationMode = justificationMode,
        onAnchorClick = onAnchorClick,
        onExternalLinkClick = onExternalLinkClick,
        onNonLinkClick = onNonLinkClick,
        onViewChanged = { textView = it },
    )
}
