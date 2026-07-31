package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock

@Composable
internal fun ProseDisclosure(
    blockKey: String,
    semantic: BookDocumentBlockContent.Disclosure,
    body: List<PreparedBookDocumentBlock>,
    resourceLoader: BookPublicationResourceLoader?,
    foreground: Color,
    background: Color,
    readerTypeface: Typeface,
    readerTextSizeSp: Float,
    lineSpacingMultiplier: Float,
    readerTextAlignment: Int,
    justificationMode: Int,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    anchorOffsetWithinBlock: Int?,
    onAnchorTargetPositioned: (LayoutCoordinates, Int) -> Unit,
    onHiddenContentChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    var expanded by rememberSaveable(blockKey) { mutableStateOf(semantic.initiallyExpanded) }
    val hiddenChildren = remember(blockKey) { mutableStateMapOf<String, Boolean>() }
    val hasHiddenContent = !expanded || hiddenChildren.values.any { it }
    LaunchedEffect(hasHiddenContent, onHiddenContentChanged) {
        onHiddenContentChanged(hasHiddenContent)
    }
    val bodyTarget = anchorOffsetWithinBlock?.let { offset ->
        resolveProseDisclosureAnchorTarget(semantic, body, offset)
    }
    LaunchedEffect(anchorOffsetWithinBlock, bodyTarget) {
        if (bodyTarget != null) expanded = true
    }
    val summaryTypefaces by rememberInlineProseTypefaces(
        loader = resourceLoader,
        styles = semantic.summary.inlineStyles,
    )
    val summaryText = remember(semantic.summary, summaryTypefaces) {
        semantic.summary.toSpanned(summaryTypefaces)
    }
    val summaryAnchorOffset = anchorOffsetWithinBlock
        ?.takeIf { bodyTarget == null }
        ?.coerceIn(0, semantic.summary.text.length)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = if (expanded) "▾ " else "▸ ",
                modifier = Modifier.clickable { expanded = !expanded },
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = readerTextSizeSp.sp,
            )
            ProseRichText(
                text = summaryText,
                documentTextIdentity = buildString {
                    append(blockKey)
                    append(":summary:")
                    append(semantic.summary.inlineStyles.hashCode())
                    append(':')
                    append(summaryTypefaces.keys.sorted().joinToString())
                },
                textColor = foreground.toArgbValue(),
                textSizeSp = readerTextSizeSp,
                typeface = Typeface.create(readerTypeface, Typeface.BOLD),
                lineSpacingMultiplier = lineSpacingMultiplier,
                textAlignment = readerTextAlignment,
                justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onNonLinkClick = { expanded = !expanded },
                anchorCharacterOffset = summaryAnchorOffset,
                onAnchorTargetPositioned = onAnchorTargetPositioned,
                modifier = Modifier.weight(1f),
            )
        }
        if (expanded) {
            body.forEachIndexed { index, content ->
                ProseDocumentBlock(
                    content = content,
                    resourceLoader = resourceLoader,
                    readerForeground = foreground,
                    readerBackground = background,
                    readerTypeface = readerTypeface,
                    readerTextSizeSp = readerTextSizeSp,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    readerTextAlignment = readerTextAlignment,
                    justificationMode = justificationMode,
                    trimTerminalLine = index != body.lastIndex,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    onViewChanged = {},
                    anchorOffsetWithinBlock = if (content == bodyTarget?.block) {
                        bodyTarget.offsetWithinBlock
                    } else {
                        null
                    },
                    onAnchorTargetPositioned = onAnchorTargetPositioned,
                    onHiddenContentChanged = { hidden ->
                        hiddenChildren[content.block.id.value] = hidden
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            bottom = if (index == body.lastIndex) 10.dp else 0.dp,
                        ),
                )
            }
        }
    }
}
