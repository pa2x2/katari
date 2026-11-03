package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalBulletListHandler
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownBulletList
import com.mikepenz.markdown.compose.elements.MarkdownDivider
import com.mikepenz.markdown.compose.elements.MarkdownOrderedList
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.compose.elements.listDepth
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownInlineContent
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownAlertPadding
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.NoOpImageTransformerImpl
import com.mikepenz.markdown.model.markdownAlertPadding
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.rememberMarkdownState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Image
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.MarkdownStyle
import mihon.presentation.kit.content.markdownStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import org.intellij.markdown.MarkdownTokenTypes.Companion.HTML_TAG
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.CommonMarkdownConstraints
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.AtxHeaderProvider
import org.intellij.markdown.parser.markerblocks.providers.BlockQuoteProvider
import org.intellij.markdown.parser.markerblocks.providers.CodeBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.CodeFenceProvider
import org.intellij.markdown.parser.markerblocks.providers.HorizontalRuleProvider
import org.intellij.markdown.parser.markerblocks.providers.ListMarkerProvider
import org.intellij.markdown.parser.markerblocks.providers.SetextHeaderProvider

const val MARKDOWN_INLINE_IMAGE_TAG = "MARKDOWN_INLINE_IMAGE"

@Composable
fun MarkdownRender(
    content: String,
    modifier: Modifier = Modifier,
    flavour: MarkdownFlavourDescriptor = SimpleMarkdownFlavourDescriptor,
    annotator: MarkdownAnnotator = remember { markdownAnnotator() },
    loadImages: Boolean = true,
    listBlockSpacing: Dp = 0.dp,
    listItemSpacing: Dp = 0.dp,
    listSectionSpacing: Dp = 0.dp,
    /** Renders body text in the secondary content colour, for text that supports a screen, such as a description. */
    secondary: Boolean = false,
) {
    val normalizedContent = remember(content) { normalizeMarkdownLineEndings(content) }
    val style = markdownStyle(secondary)
    val padding = remember(listBlockSpacing, listItemSpacing) {
        getMarkdownPadding(listBlockSpacing, listItemSpacing)
    }
    val components = remember(listSectionSpacing) { getMarkdownComponents(listSectionSpacing) }

    Markdown(
        markdownState = rememberMarkdownState(
            content = normalizedContent,
            flavour = flavour,
            immediate = false,
        ),
        annotator = annotator,
        colors = getMarkdownColors(style),
        typography = getMarkdownTypography(style),
        padding = padding,
        components = components,
        imageTransformer = remember(loadImages) {
            if (loadImages) Coil3ImageTransformerImpl else NoOpImageTransformerImpl()
        },
        inlineContent = getMarkdownInlineContent(style),
        modifier = modifier,
    )
}

private fun getMarkdownColors(style: MarkdownStyle): MarkdownColors {
    return DefaultMarkdownColors(
        text = style.textColor,
        codeBackground = style.codeBackground,
        inlineCodeBackground = style.codeBackground,
        dividerColor = style.dividerColor,
        tableBackground = style.tableBackground,
        highlightText = style.highlightText,
        highlightBackground = style.highlightBackground,
    )
}

private fun getMarkdownTypography(style: MarkdownStyle): MarkdownTypography {
    val (h1, h2, h3, h4, h5) = style.headings
    return DefaultMarkdownTypography(
        h1 = h1,
        h2 = h2,
        h3 = h3,
        h4 = h4,
        h5 = h5,
        h6 = style.headings[5],
        text = style.body,
        code = style.code,
        inlineCode = style.code,
        quote = style.quote,
        paragraph = style.body,
        ordered = style.body,
        bullet = style.body,
        list = style.body,
        textLink = TextLinkStyles(style = style.link),
        table = style.body,
    )
}

private fun getMarkdownPadding(listBlockSpacing: Dp, listItemSpacing: Dp) = object : MarkdownPadding {
    override val block: Dp = KatariSpacing.xxs
    override val blockQuote: PaddingValues = PaddingValues(horizontal = KatariSpacing.l)
    override val blockQuoteBar: PaddingValues.Absolute = PaddingValues.Absolute(
        left = KatariSpacing.xs,
        top = KatariSpacing.xxs,
        right = KatariSpacing.xs,
        bottom = KatariSpacing.xxs,
    )
    override val blockQuoteText: PaddingValues = PaddingValues(vertical = KatariSpacing.xs)
    override val codeBlock: PaddingValues = PaddingValues(KatariSpacing.s)
    override val alert: MarkdownAlertPadding = markdownAlertPadding()
    override val list: Dp = listBlockSpacing
    override val listIndent: Dp = KatariSpacing.s
    override val listItemBottom: Dp = listItemSpacing
    override val listItemTop: Dp = 0.dp
}

internal fun normalizeMarkdownLineEndings(content: String): String {
    return content.replace("\r\n", "\n").replace('\r', '\n')
}

private fun getMarkdownComponents(listSectionSpacing: Dp) = markdownComponents(
    horizontalRule = {
        MarkdownDivider(
            modifier = Modifier
                .padding(vertical = KatariTheme.spacing.xs)
                .fillMaxWidth(),
        )
    },
    orderedList = { ol ->
        Column(
            modifier = Modifier.padding(
                start = KatariTheme.spacing.s,
                bottom = listSectionSpacing,
            ),
        ) {
            MarkdownOrderedList(
                content = ol.content,
                node = ol.node,
                style = ol.typography.ordered,
                depth = ol.listDepth,
                markerModifier = { Modifier.alignBy(FirstBaseline) },
                listModifier = { Modifier.alignBy(FirstBaseline) },
            )
        }
    },
    unorderedList = { ul ->
        val markers = listOf("•", "◦", "▸", "▹")

        CompositionLocalProvider(
            LocalBulletListHandler provides { _, _, _, _, _ -> "${markers[ul.listDepth % markers.size]} " },
        ) {
            Column(
                modifier = Modifier.padding(
                    start = KatariTheme.spacing.s,
                    bottom = listSectionSpacing,
                ),
            ) {
                MarkdownBulletList(
                    content = ul.content,
                    node = ul.node,
                    style = ul.typography.bullet,
                    markerModifier = { Modifier.alignBy(FirstBaseline) },
                    listModifier = { Modifier.alignBy(FirstBaseline) },
                )
            }
        }
    },
    table = { t ->
        MarkdownTable(
            content = t.content,
            node = t.node,
            style = t.typography.text,
            headerBlock = { content, header, tableWidth, style ->
                MarkdownTableHeader(
                    content = content,
                    header = header,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                )
            },
            rowBlock = { content, header, tableWidth, style ->
                MarkdownTableRow(
                    content = content,
                    header = header,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                )
            },
        )
    },
    custom = { type, model ->
        if (type in DISALLOWED_MARKDOWN_TYPES) {
            MarkdownText(
                content = model.content.substring(model.node.startOffset, model.node.endOffset),
                node = model.node,
                style = model.typography.text,
            )
        }
    },
)

private fun getMarkdownInlineContent(style: MarkdownStyle) = DefaultMarkdownInlineContent(
    inlineContent = mapOf(
        MARKDOWN_INLINE_IMAGE_TAG to InlineTextContent(
            placeholder = Placeholder(
                width = style.inlineImageSize,
                height = style.inlineImageSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
            children = {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Image,
                    contentDescription = null,
                    tone = ContentTone.Primary,
                )
            },
        ),
    ),
)

private object SimpleMarkdownFlavourDescriptor : CommonMarkFlavourDescriptor() {
    override val markerProcessorFactory: MarkerProcessorFactory = SimpleMarkdownProcessFactory
}

private object SimpleMarkdownProcessFactory : MarkerProcessorFactory {
    override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> {
        return SimpleMarkdownMarkerProcessor(productionHolder, CommonMarkdownConstraints.BASE)
    }
}

/**
 * Like `CommonMarkFlavour`, but with html blocks and reference links removed and
 * table support added
 */
private class SimpleMarkdownMarkerProcessor(
    productionHolder: ProductionHolder,
    constraints: MarkdownConstraints,
) : CommonMarkMarkerProcessor(productionHolder, constraints) {
    private val markerBlockProviders = listOf(
        CodeBlockProvider(),
        HorizontalRuleProvider(),
        CodeFenceProvider(),
        SetextHeaderProvider(),
        BlockQuoteProvider(),
        ListMarkerProvider(),
        AtxHeaderProvider(),
        GitHubTableMarkerProvider(),
    )

    override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> {
        return markerBlockProviders
    }
}

val DISALLOWED_MARKDOWN_TYPES = arrayOf(HTML_TAG)
