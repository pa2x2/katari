package mihon.entry.interactions.book.prose

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBorderStyle
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentLink
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentTableCellScope
import mihon.entry.interactions.book.document.model.layoutBookDocumentTable
import mihon.entry.interactions.book.document.reader.BookDocumentResourceLoader
import mihon.entry.interactions.book.document.reader.BookDocumentText
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.createValidatedProseTypeface
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

@Composable
internal fun ProseDocumentBlock(
    content: PreparedBookDocumentBlock,
    resourceLoader: BookDocumentResourceLoader?,
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
        content.renderedText.withInlineDocumentStyles(content.block.inlineStyles, inlineTypefaces)
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

@Composable
private fun ProseFigure(
    semantic: BookDocumentBlockContent.Figure,
    resourceLoader: BookDocumentResourceLoader?,
    foreground: Color,
    background: Color,
    modifier: Modifier,
) {
    val aspectRatio = semantic.image.width
        ?.takeIf { it > 0 }
        ?.toFloat()
        ?.div(semantic.image.height?.takeIf { it > 0 } ?: semantic.image.width)
        ?.coerceIn(MIN_IMAGE_ASPECT_RATIO, MAX_IMAGE_ASPECT_RATIO)
        ?: DEFAULT_IMAGE_ASPECT_RATIO
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val targetHeightPx = (targetWidthPx / aspectRatio).roundToInt().coerceAtLeast(1)
        val image by produceState<LoadedProseImage?>(
            initialValue = null,
            semantic.image.resourceId,
            resourceLoader,
            targetWidthPx,
            targetHeightPx,
        ) {
            value = resourceLoader?.loadProseImage(
                resourceId = semantic.image.resourceId,
                targetWidthPx = targetWidthPx,
                targetHeightPx = targetHeightPx,
            )?.getOrNull()
        }
        val ownedBitmap = (image as? LoadedProseImage.Success)?.bitmap
        DisposableEffect(ownedBitmap) {
            onDispose {
                ownedBitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
                color = foreground.copy(alpha = 0.06f).compositeOver(background),
                contentColor = foreground,
            ) {
                when (val loaded = image) {
                    null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (resourceLoader == null) {
                            Text(semantic.image.alternativeText ?: IMAGE_UNAVAILABLE_TEXT)
                        } else {
                            CircularProgressIndicator(color = foreground)
                        }
                    }
                    is LoadedProseImage.Success -> Image(
                        bitmap = loaded.bitmap.asImageBitmap(),
                        contentDescription = semantic.image.alternativeText,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                    is LoadedProseImage.Failure -> Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(semantic.image.alternativeText ?: IMAGE_UNAVAILABLE_TEXT)
                    }
                }
            }
            semantic.caption?.let { caption ->
                Text(
                    text = caption,
                    color = foreground.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProseTable(
    semantic: BookDocumentBlockContent.Table,
    documentTextIdentityPrefix: String,
    foreground: Color,
    background: Color,
    readerTypeface: Typeface,
    readerTextSizeSp: Float,
    lineSpacingMultiplier: Float,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
    anchorOffsetWithinBlock: Int?,
    onAnchorTargetPositioned: (LayoutCoordinates, Int) -> Unit,
    modifier: Modifier,
) {
    val rows = remember(semantic) { semantic.toDisplayRows() }
    val anchorTarget = remember(semantic, anchorOffsetWithinBlock) {
        anchorOffsetWithinBlock?.let { semantic.resolveProseTableAnchorTarget(it) }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        semantic.caption?.let { caption ->
            ProseTableText(
                text = caption.toSpanned(
                    links = semantic.captionLinks,
                    inlineStyles = inlineStyles.within(0, caption.length),
                    inlineTypefaces = inlineTypefaces,
                ),
                documentTextIdentity = buildString {
                    append(documentTextIdentityPrefix)
                    append(":caption:")
                    append(inlineStyles.hashCode())
                    append(':')
                    append(inlineTypefaces.keys.sorted().joinToString())
                },
                textColor = foreground.toArgbValue(),
                textSizeSp = readerTextSizeSp,
                typeface = Typeface.create(readerTypeface, Typeface.BOLD),
                lineSpacingMultiplier = lineSpacingMultiplier,
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START,
                justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                anchorCharacterOffset = (anchorTarget as? ProseTableAnchorTarget.Caption)?.characterOffset,
                onAnchorTargetPositioned = onAnchorTargetPositioned,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .border(1.dp, foreground.copy(alpha = 0.35f)),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row {
                    row.cells.forEach { cell ->
                        val cellBackground = if (cell.header) {
                            foreground.copy(alpha = 0.12f).compositeOver(background)
                        } else {
                            background
                        }
                        val cellTarget = (anchorTarget as? ProseTableAnchorTarget.Cell)
                            ?.takeIf {
                                it.rowIndex == rowIndex &&
                                    it.cellIndex == cell.sourceCellIndex
                            }
                        ProseTableText(
                            text = cell.displayText.toSpanned(
                                links = cell.links,
                                inlineStyles = inlineStyles.within(
                                    cell.logicalStart,
                                    cell.logicalEndExclusive,
                                ),
                                inlineTypefaces = inlineTypefaces,
                            ),
                            documentTextIdentity = buildString {
                                append(documentTextIdentityPrefix)
                                append(':')
                                append(cell.logicalStart)
                                append(':')
                                append(cell.logicalEndExclusive)
                                append(':')
                                append(inlineStyles.hashCode())
                                append(':')
                                append(inlineTypefaces.keys.sorted().joinToString())
                            },
                            modifier = Modifier
                                .width(TABLE_COLUMN_WIDTH * cell.columnSpan)
                                .background(cellBackground)
                                .border(0.5.dp, foreground.copy(alpha = 0.25f))
                                .then(
                                    if (cell.header) {
                                        Modifier.semantics {
                                            heading()
                                            contentDescription = when (cell.scope) {
                                                BookDocumentTableCellScope.ROW -> "Row header: ${cell.displayText}"
                                                BookDocumentTableCellScope.COLUMN ->
                                                    "Column header: ${cell.displayText}"
                                                BookDocumentTableCellScope.ROW_GROUP ->
                                                    "Row group header: ${cell.displayText}"
                                                BookDocumentTableCellScope.COLUMN_GROUP ->
                                                    "Column group header: ${cell.displayText}"
                                                null -> "Table header: ${cell.displayText}"
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(8.dp),
                            textColor = foreground.toArgbValue(),
                            textSizeSp = readerTextSizeSp * TABLE_TEXT_SCALE,
                            typeface = if (cell.header) {
                                Typeface.create(readerTypeface, Typeface.BOLD)
                            } else {
                                readerTypeface
                            },
                            lineSpacingMultiplier = lineSpacingMultiplier,
                            textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START,
                            justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            anchorCharacterOffset = cellTarget?.characterOffset,
                            onAnchorTargetPositioned = onAnchorTargetPositioned,
                        )
                    }
                }
                if (rowIndex != rows.lastIndex) {
                    HorizontalDivider(color = foreground.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ProseTableText(
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
        onViewChanged = { textView = it },
    )
}

@Composable
private fun ProseDisclosure(
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

internal data class ProseDisclosureAnchorTarget(
    val block: PreparedBookDocumentBlock,
    val offsetWithinBlock: Int,
)

internal fun resolveProseDisclosureAnchorTarget(
    summary: String,
    body: List<PreparedBookDocumentBlock>,
    offsetWithinDisclosure: Int,
): ProseDisclosureAnchorTarget? {
    val bodyOffset = offsetWithinDisclosure - summary.length - 1
    if (bodyOffset < 0) return null
    val block = body.firstOrNull { candidate ->
        bodyOffset >= candidate.block.logicalStart &&
            (
                bodyOffset < candidate.block.logicalEndExclusive ||
                    (
                        candidate == body.last() &&
                            bodyOffset == candidate.block.logicalEndExclusive
                        )
                )
    } ?: return null
    return ProseDisclosureAnchorTarget(
        block = block,
        offsetWithinBlock = (bodyOffset - block.block.logicalStart)
            .coerceIn(0, block.block.logicalLength),
    )
}

@Composable
private fun rememberProseTypeface(
    loader: BookDocumentResourceLoader?,
    family: BookDocumentFontFamily.Resource?,
): androidx.compose.runtime.State<Typeface?> {
    val context = LocalContext.current
    return produceState<Typeface?>(
        initialValue = null,
        loader,
        family?.resourceId,
    ) {
        val resourceId = family?.resourceId ?: return@produceState
        value = loader?.loadProseTypeface(context, resourceId)?.getOrNull()
    }
}

@Composable
private fun rememberInlineProseTypefaces(
    loader: BookDocumentResourceLoader?,
    styles: List<BookDocumentInlineStyleRange>,
): androidx.compose.runtime.State<Map<String, Typeface>> {
    val context = LocalContext.current
    val resourceIds = styles.mapNotNullTo(linkedSetOf()) { inline ->
        (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
    }
    return produceState(
        initialValue = emptyMap(),
        loader,
        resourceIds,
    ) {
        if (loader == null) return@produceState
        val loaded = mutableMapOf<String, Typeface>()
        resourceIds.forEach { resourceId ->
            loader.loadProseTypeface(context, resourceId).getOrNull()?.let { typeface ->
                loaded[resourceId] = typeface
                value = loaded.toMap()
            }
        }
    }
}

private fun Spanned.withInlineDocumentStyles(
    styles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
): Spanned {
    if (styles.isEmpty()) return this
    return SpannableString(this).apply {
        styles.forEach { range ->
            val start = range.start.coerceIn(0, length)
            val end = range.endExclusive.coerceIn(start, length)
            if (end <= start) return@forEach
            val spanFlags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            range.style.foregroundArgb?.let {
                setSpan(ForegroundColorSpan(it.toInt()), start, end, spanFlags)
            }
            range.style.backgroundArgb?.let {
                setSpan(BackgroundColorSpan(it.toInt()), start, end, spanFlags)
            }
            range.style.fontSizeScale?.let {
                setSpan(RelativeSizeSpan(it), start, end, spanFlags)
            }
            if (range.style.bold) {
                setSpan(StyleSpan(Typeface.BOLD), start, end, spanFlags)
            }
            when (val family = range.style.fontFamily) {
                is BookDocumentFontFamily.Generic -> {
                    val name = when (family.family) {
                        BookDocumentFontFamily.GenericFamily.SERIF -> "serif"
                        BookDocumentFontFamily.GenericFamily.SANS_SERIF -> "sans-serif"
                        BookDocumentFontFamily.GenericFamily.MONOSPACE -> "monospace"
                    }
                    setSpan(TypefaceSpan(name), start, end, spanFlags)
                }
                is BookDocumentFontFamily.Resource -> inlineTypefaces[family.resourceId]?.let { typeface ->
                    setSpan(ProseTypefaceSpan(typeface), start, end, spanFlags)
                }
                null -> Unit
            }
        }
    }
}

private class ProseTypefaceSpan(
    private val typeface: Typeface,
) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)
    override fun updateMeasureState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)

    private fun Paint.applyTypeface(newTypeface: Typeface) {
        val previousStyle = typeface?.style ?: Typeface.NORMAL
        val missingStyle = previousStyle and newTypeface.style.inv()
        if (missingStyle and Typeface.BOLD != 0) isFakeBoldText = true
        if (missingStyle and Typeface.ITALIC != 0) textSkewX = -0.25f
        typeface = newTypeface
    }
}

private suspend fun BookDocumentResourceLoader.loadProseImage(
    resourceId: String,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Result<LoadedProseImage> {
    return try {
        val resource = load(
            resourceId,
            PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
        ).getOrThrow()
        Result.success(
            withContext(Dispatchers.Default) {
                LoadedProseImage.Success(
                    decodeValidatedProseImage(resource.bytes, targetWidthPx, targetHeightPx),
                )
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        Result.success(LoadedProseImage.Failure)
    }
}

internal suspend fun BookDocumentResourceLoader.loadProseTypeface(
    context: android.content.Context,
    resourceId: String,
): Result<Typeface> {
    return try {
        val resource = load(
            resourceId,
            PROSE_FONT_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            PROSE_FONT_RESOURCE_REQUIREMENT.maxBytes,
        ).getOrThrow()
        Result.success(
            withContext(Dispatchers.IO) {
                synchronized(proseFontCacheLock) {
                    val directory = File(context.cacheDir, "prose-fonts").apply { mkdirs() }
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest(resource.bytes)
                        .joinToString("") { byte -> "%02x".format(byte) }
                    val target = File(directory, digest)
                    pruneProseFontCache(directory, target)
                    if (!target.isFile || target.length() != resource.bytes.size.toLong()) {
                        target.outputStream().use { it.write(resource.bytes) }
                    }
                    target.setLastModified(System.currentTimeMillis())
                    createValidatedProseTypeface(target)
                }
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun pruneProseFontCache(directory: File, retained: File) {
    val files = directory.listFiles()
        .orEmpty()
        .filter(File::isFile)
        .sortedByDescending(File::lastModified)
    var retainedBytes = 0L
    var retainedFiles = 0
    files.forEach { file ->
        if (file == retained ||
            (retainedFiles < MAX_FONT_CACHE_FILES && retainedBytes + file.length() <= MAX_FONT_CACHE_BYTES)
        ) {
            retainedFiles++
            retainedBytes += file.length()
        } else {
            file.delete()
        }
    }
}

private fun BookDocumentBlockContent.Table.toDisplayRows(): List<DisplayTableRow> {
    val layout = checkNotNull(rows.layoutBookDocumentTable())
    check(layout.columnCount == columnCount)
    var logicalOffset = caption?.let { it.length + 1 } ?: 0
    return layout.rows.mapIndexed { rowIndex, row ->
        val modelRow = rows[rowIndex]
        val logicalRanges = modelRow.cells.mapIndexed { cellIndex, cell ->
            if (cellIndex > 0) logicalOffset++
            val start = logicalOffset
            logicalOffset += cell.text.length
            start until logicalOffset
        }
        val placements = row.placements.mapIndexed { cellIndex, placement ->
            placement.column to (placement to cellIndex)
        }.toMap()
        val displayed = mutableListOf<DisplayTableCell>()
        var column = 0
        while (column < columnCount) {
            val placed = placements[column]
            if (placed != null) {
                val (placement, sourceCellIndex) = placed
                val cell = placement.cell
                val logicalRange = logicalRanges[sourceCellIndex]
                displayed += DisplayTableCell(
                    displayText = cell.text.ifEmpty { " " },
                    header = cell.header,
                    scope = cell.scope,
                    columnSpan = cell.columnSpan,
                    links = cell.links,
                    sourceCellIndex = sourceCellIndex,
                    logicalStart = logicalRange.first,
                    logicalEndExclusive = logicalRange.last + 1,
                )
                column += cell.columnSpan
                continue
            }
            displayed += DisplayTableCell(
                displayText = if (column in row.carriedColumns) "↳" else " ",
                header = false,
                scope = null,
                columnSpan = 1,
                links = emptyList(),
                sourceCellIndex = -1,
                logicalStart = logicalOffset,
                logicalEndExclusive = logicalOffset,
            )
            column++
        }
        logicalOffset++
        DisplayTableRow(displayed)
    }
}

internal sealed interface ProseTableAnchorTarget {
    data class Caption(val characterOffset: Int) : ProseTableAnchorTarget
    data class Cell(
        val rowIndex: Int,
        val cellIndex: Int,
        val characterOffset: Int,
    ) : ProseTableAnchorTarget
}

internal fun BookDocumentBlockContent.Table.resolveProseTableAnchorTarget(
    offsetWithinBlock: Int,
): ProseTableAnchorTarget {
    val target = offsetWithinBlock.coerceAtLeast(0)
    var logicalOffset = 0
    caption?.let { value ->
        if (target <= value.length) {
            return ProseTableAnchorTarget.Caption(target.coerceIn(0, value.length))
        }
        logicalOffset = value.length + 1
    }
    rows.forEachIndexed { rowIndex, row ->
        val cells = row.cells.mapIndexed { cellIndex, cell ->
            if (cellIndex > 0) logicalOffset++
            val start = logicalOffset
            logicalOffset += cell.text.length
            Triple(cellIndex, start, logicalOffset)
        }
        if (target <= logicalOffset) {
            val selected = cells.lastOrNull { (_, start) -> target >= start } ?: cells.first()
            return ProseTableAnchorTarget.Cell(
                rowIndex = rowIndex,
                cellIndex = selected.first,
                characterOffset = (target - selected.second).coerceIn(0, selected.third - selected.second),
            )
        }
        logicalOffset++
    }
    val lastRowIndex = rows.lastIndex
    val lastCellIndex = rows.last().cells.lastIndex
    return ProseTableAnchorTarget.Cell(
        rowIndex = lastRowIndex,
        cellIndex = lastCellIndex,
        characterOffset = rows.last().cells.last().text.length,
    )
}

private fun String.toSpanned(
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
): Spanned =
    SpannableString(this).apply {
        links.forEach { link ->
            val url = when (val target = link.target) {
                is BookDocumentLinkTarget.Anchor -> "#${target.fragment}"
                is BookDocumentLinkTarget.External -> target.url
            }
            setSpan(
                URLSpan(url),
                link.start,
                link.endExclusive,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }.withInlineDocumentStyles(inlineStyles, inlineTypefaces)

private fun List<BookDocumentInlineStyleRange>.within(
    start: Int,
    endExclusive: Int,
): List<BookDocumentInlineStyleRange> = mapNotNull { inline ->
    val clippedStart = maxOf(inline.start, start)
    val clippedEnd = minOf(inline.endExclusive, endExclusive)
    if (clippedEnd <= clippedStart) {
        null
    } else {
        inline.copy(
            start = clippedStart - start,
            endExclusive = clippedEnd - start,
        )
    }
}

private fun BookDocumentStyle.borderModifier(fallbackColor: Color): Modifier {
    val border = border ?: return Modifier
    val color = border.colorArgb?.toComposeColor() ?: fallbackColor.copy(alpha = 0.55f)
    return Modifier.drawBehind {
        val width = border.widthDp.dp.toPx()
        val pathEffect = when (border.style) {
            BookDocumentBorderStyle.SOLID -> null
            BookDocumentBorderStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(width * 4f, width * 3f))
            BookDocumentBorderStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(width, width * 2f))
        }
        drawRect(
            color = color,
            style = Stroke(width = width, pathEffect = pathEffect),
        )
    }
}

private fun BookDocumentAlignment?.toTextViewAlignment(): Int? = when (this) {
    BookDocumentAlignment.START -> TextView.TEXT_ALIGNMENT_VIEW_START
    BookDocumentAlignment.CENTER -> TextView.TEXT_ALIGNMENT_CENTER
    BookDocumentAlignment.END -> TextView.TEXT_ALIGNMENT_VIEW_END
    null -> null
}

private fun Long.toComposeColor(): Color = Color(toInt())

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun contrastingTextColor(background: Color): Color =
    if (background.luminance() > 0.45f) Color.Black else Color.White

private fun Color.toArgbValue(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

private sealed interface LoadedProseImage {
    /** The composing [ProseFigure] exclusively owns and recycles this bitmap. */
    data class Success(val bitmap: Bitmap) : LoadedProseImage
    data object Failure : LoadedProseImage
}

private data class DisplayTableRow(
    val cells: List<DisplayTableCell>,
)

private data class DisplayTableCell(
    val displayText: String,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val links: List<BookDocumentLink>,
    val sourceCellIndex: Int,
    val logicalStart: Int,
    val logicalEndExclusive: Int,
)

private val TABLE_COLUMN_WIDTH = 150.dp
private const val IMAGE_UNAVAILABLE_TEXT = "Image unavailable"
private const val MAX_FONT_CACHE_FILES = 8
private const val MAX_FONT_CACHE_BYTES = 16L * 1024L * 1024L
private const val MIN_IMAGE_ASPECT_RATIO = 0.25f
private const val MAX_IMAGE_ASPECT_RATIO = 4f
private const val DEFAULT_IMAGE_ASPECT_RATIO = 4f / 3f
private const val MIN_TEXT_CONTRAST = 4.5f
private const val TABLE_TEXT_SCALE = 0.875f
private val proseFontCacheLock = Any()
