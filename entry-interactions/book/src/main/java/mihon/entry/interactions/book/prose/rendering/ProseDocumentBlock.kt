package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.document.reader.BookDocumentText
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock

@Composable
internal fun ProseDocumentBlock(
    content: PreparedBookDocumentBlock,
    resourceLoader: BookPublicationResourceLoader?,
    readerForeground: Color,
    readerBackground: Color,
    readerTypeface: Typeface,
    readerTextSizeSp: Float,
    lineSpacingMultiplier: Float,
    readerTextAlignment: Int,
    justificationMode: Int,
    trimTerminalLine: Boolean,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onViewChanged: (TextView?) -> Unit,
    anchorOffsetWithinBlock: Int? = null,
    onAnchorTargetPositioned: (LayoutCoordinates, Int) -> Unit = { _, _ -> },
    onHiddenContentChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val style = content.block.style
    val blockBackground = style.backgroundArgb?.toComposeColor()?.compositeOver(readerBackground) ?: readerBackground
    val requestedForeground = style.foregroundArgb?.toComposeColor()?.compositeOver(blockBackground)
    val blockForeground = requestedForeground
        ?.takeIf { contrastRatio(it, blockBackground) >= MIN_TEXT_CONTRAST }
        ?: readerForeground.takeIf { contrastRatio(it, blockBackground) >= MIN_TEXT_CONTRAST }
        ?: contrastingTextColor(blockBackground)
    val baseTypeface = when (val family = style.fontFamily) {
        is BookDocumentFontFamily.Generic -> when (family.family) {
            BookDocumentFontFamily.GenericFamily.SERIF -> Typeface.SERIF
            BookDocumentFontFamily.GenericFamily.SANS_SERIF -> Typeface.SANS_SERIF
            BookDocumentFontFamily.GenericFamily.MONOSPACE -> Typeface.MONOSPACE
        }
        else -> readerTypeface
    }
    val customTypeface by rememberProseTypeface(
        loader = resourceLoader,
        family = style.fontFamily as? BookDocumentFontFamily.Resource,
    )
    val typeface = customTypeface ?: baseTypeface
    val inlineTypefaces by rememberInlineProseTypefaces(
        loader = resourceLoader,
        styles = content.block.inlineStyles,
    )
    val styledRenderedText = remember(content.renderedText, content.block.inlineStyles, inlineTypefaces) {
        content.renderedText.withInlineDocumentTypefaces(content.block.inlineStyles, inlineTypefaces)
    }
    var anchorTextView by remember(content.block.id, anchorOffsetWithinBlock) {
        mutableStateOf<TextView?>(null)
    }
    var anchorCoordinates by remember(content.block.id, anchorOffsetWithinBlock) {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val tracksAnchorDirectly =
        anchorOffsetWithinBlock != null &&
            content.block.content !is BookDocumentBlockContent.Disclosure &&
            content.block.content !is BookDocumentBlockContent.Table
    LaunchedEffect(
        anchorOffsetWithinBlock,
        anchorTextView,
        anchorCoordinates,
        tracksAnchorDirectly,
        onAnchorTargetPositioned,
    ) {
        if (!tracksAnchorDirectly) return@LaunchedEffect
        val coordinates = anchorCoordinates?.takeIf { it.isAttached } ?: return@LaunchedEffect
        val offsetPx = anchorTextView?.let { view ->
            val layout = view.layout ?: return@let 0
            val characterOffset = anchorOffsetWithinBlock.coerceIn(0, view.text.length)
            layout.getLineTop(layout.getLineForOffset(characterOffset))
        } ?: 0
        onAnchorTargetPositioned(coordinates, offsetPx)
    }
    val styledModifier = modifier
        .then(style.backgroundArgb?.let { Modifier.background(blockBackground) } ?: Modifier)
        .then(style.borderModifier(blockForeground))
        .then(
            if (style.paddingEm > 0f) {
                Modifier.padding((style.paddingEm * readerTextSizeSp).dp)
            } else {
                Modifier
            },
        )
    val renderedModifier = styledModifier.then(
        if (tracksAnchorDirectly) {
            Modifier.onGloballyPositioned { anchorCoordinates = it }
        } else {
            Modifier
        },
    )
    LaunchedEffect(content.block.id, content.block.content, onHiddenContentChanged) {
        if (content.block.content !is BookDocumentBlockContent.Disclosure) {
            onHiddenContentChanged(false)
        }
    }

    when (val semantic = content.block.content) {
        is BookDocumentBlockContent.Text,
        is BookDocumentBlockContent.ListBlock,
        -> BookDocumentText(
            text = styledRenderedText,
            documentTextIdentity = buildString {
                append(content.block.id.value)
                append(':')
                append(content.block.inlineStyles.hashCode())
                append(':')
                append(inlineTypefaces.keys.sorted().joinToString())
            },
            textColor = blockForeground.toArgbValue(),
            textSizeSp = readerTextSizeSp * style.fontSizeScale,
            typeface = if (style.bold) Typeface.create(typeface, Typeface.BOLD) else typeface,
            lineSpacingMultiplier = lineSpacingMultiplier,
            textAlignment = style.alignment.toTextViewAlignment() ?: readerTextAlignment,
            justificationMode = if (style.alignment != null) Layout.JUSTIFICATION_MODE_NONE else justificationMode,
            trimTerminalLine = trimTerminalLine,
            onAnchorClick = onAnchorClick,
            onExternalLinkClick = onExternalLinkClick,
            onViewChanged = { view ->
                anchorTextView = view
                onViewChanged(view)
            },
            modifier = renderedModifier,
        )
        BookDocumentBlockContent.ThematicBreak -> {
            onViewChanged(null)
            Box(renderedModifier.padding(vertical = 18.dp)) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = blockForeground.copy(alpha = 0.55f),
                    thickness = 1.dp,
                )
            }
        }
        is BookDocumentBlockContent.Figure -> {
            onViewChanged(null)
            ProseFigure(
                semantic = semantic,
                resourceLoader = resourceLoader,
                foreground = blockForeground,
                background = blockBackground,
                documentTextIdentityPrefix = content.block.id.value,
                readerTypeface = typeface,
                readerTextSizeSp = readerTextSizeSp * style.fontSizeScale,
                lineSpacingMultiplier = lineSpacingMultiplier,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                modifier = renderedModifier,
            )
        }
        is BookDocumentBlockContent.Table -> {
            onViewChanged(null)
            ProseTable(
                semantic = semantic,
                documentTextIdentityPrefix = content.block.id.value,
                foreground = blockForeground,
                background = blockBackground,
                readerTypeface = typeface,
                readerTextSizeSp = readerTextSizeSp * style.fontSizeScale,
                lineSpacingMultiplier = lineSpacingMultiplier,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                inlineStyles = content.block.inlineStyles,
                inlineTypefaces = inlineTypefaces,
                anchorOffsetWithinBlock = anchorOffsetWithinBlock,
                onAnchorTargetPositioned = onAnchorTargetPositioned,
                modifier = renderedModifier,
            )
        }
        is BookDocumentBlockContent.Disclosure -> {
            onViewChanged(null)
            ProseDisclosure(
                blockKey = content.block.id.value,
                semantic = semantic,
                body = content.disclosureBody,
                resourceLoader = resourceLoader,
                foreground = blockForeground,
                background = blockBackground,
                readerTypeface = typeface,
                readerTextSizeSp = readerTextSizeSp * style.fontSizeScale,
                lineSpacingMultiplier = lineSpacingMultiplier,
                readerTextAlignment = style.alignment.toTextViewAlignment() ?: readerTextAlignment,
                justificationMode = if (style.alignment != null) {
                    Layout.JUSTIFICATION_MODE_NONE
                } else {
                    justificationMode
                },
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                anchorOffsetWithinBlock = anchorOffsetWithinBlock,
                onAnchorTargetPositioned = onAnchorTargetPositioned,
                onHiddenContentChanged = onHiddenContentChanged,
                modifier = renderedModifier,
            )
        }
        is BookDocumentBlockContent.Unsupported -> {
            onViewChanged(null)
            Text(
                text = UNSUPPORTED_CONTENT_BLOCK_TEXT,
                modifier = renderedModifier.padding(vertical = 12.dp),
                color = blockForeground.copy(alpha = 0.8f),
                fontSize = (readerTextSizeSp * style.fontSizeScale).sp,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}
