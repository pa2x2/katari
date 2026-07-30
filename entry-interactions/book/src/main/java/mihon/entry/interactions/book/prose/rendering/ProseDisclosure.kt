package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentResourceLoader
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock

@Composable
internal fun ProseDisclosure(
    blockKey: String,
    semantic: BookDocumentBlockContent.Disclosure,
    body: List<PreparedBookDocumentBlock>,
    resourceLoader: BookDocumentResourceLoader?,
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
        resolveProseDisclosureAnchorTarget(semantic.summary, body, offset)
    }
    LaunchedEffect(anchorOffsetWithinBlock, bodyTarget) {
        if (bodyTarget != null) expanded = true
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = (if (expanded) "▾ " else "▸ ") + semantic.summary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp)
                .then(
                    if (anchorOffsetWithinBlock != null && bodyTarget == null) {
                        Modifier.onGloballyPositioned { onAnchorTargetPositioned(it, 0) }
                    } else {
                        Modifier
                    },
                ),
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = readerTextSizeSp.sp,
        )
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
