package eu.kanade.presentation.entry.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import eu.kanade.presentation.components.DISALLOWED_MARKDOWN_TYPES
import eu.kanade.presentation.components.MARKDOWN_INLINE_IMAGE_TAG
import eu.kanade.presentation.components.MarkdownRender
import eu.kanade.presentation.entry.components.preview.PreviewSizeUi
import eu.kanade.presentation.entry.components.preview.SharedEntryPreviewSection
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import mihon.app.di.appGraph
import mihon.presentation.kit.chip.SuggestionChip
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.markdownStyle
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.surface.backgroundHalo
import mihon.presentation.kit.surface.fadeIntoBackground
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import kotlin.math.roundToInt

/** An entry's notes, description and tags, collapsed to a few lines until tapped, followed by its page preview. */
@Composable
fun ExpandableEntryDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: PreviewSizeUi,
    entryPreviewState: EntryViewModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: ((Long, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        EntrySummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(top = KatariTheme.spacing.s)
                .padding(horizontal = KatariTheme.spacing.contentInset)
                .clickableNoIndication { onExpanded(!expanded) },
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            EntryTags(
                tags = tags,
                expanded = expanded,
                onTagSearch = onTagSearch,
                onCopyTagToClipboard = onCopyTagToClipboard,
            )
        }
        if (entryPreviewEnabled) {
            SharedEntryPreviewSection(
                state = entryPreviewState,
                size = entryPreviewSize,
                onExpandedChange = onPreviewExpandedChange,
                onRetry = onPreviewRetry,
                onPageLoad = onPreviewPageLoad,
                onPageClick = onPreviewPageClick,
            )
        }
    }
}

/** Tags wrap onto several lines while the description is expanded and scroll in one line while it is collapsed. */
@Composable
private fun EntryTags(
    tags: List<String>,
    expanded: Boolean,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
) {
    Box(
        modifier = Modifier
            // Chips keep their own touch margin, so these pads only add what the margin lacks.
            .padding(top = KatariTheme.spacing.l, bottom = KatariTheme.spacing.s)
            .animateContentSize(animationSpec = spring())
            .fillMaxWidth(),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var tagSelected by remember { mutableStateOf("") }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_search),
                onClick = {
                    onTagSearch(tagSelected)
                    showMenu = false
                },
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_copy_to_clipboard),
                onClick = {
                    onCopyTagToClipboard(tagSelected)
                    showMenu = false
                },
            )
        }
        val onTagClick = { tag: String ->
            tagSelected = tag
            showMenu = true
        }
        if (expanded) {
            FlowRow(
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.contentInset),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                tags.forEach { tag ->
                    SuggestionChip(label = tag, onClick = { onTagClick(tag) })
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = KatariTheme.spacing.m),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                items(items = tags) { tag ->
                    SuggestionChip(label = tag, onClick = { onTagClick(tag) })
                }
            }
        }
    }
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

                val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
                        ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
                        ?.getUnescapedTextInNode(content)
                    ?: return@markdownAnnotator false

                val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
                    ?.getUnescapedTextInNode(content).orEmpty()

                withLink(LinkAnnotation.Url(url = url)) {
                    pushStyle(linkStyle)
                    appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
                    append(altText)
                    pop()
                }

                return@markdownAnnotator true
            }

            if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                append(content.substring(child.startOffset, child.endOffset))
                return@markdownAnnotator true
            }

            false
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun EntrySummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.uiPreferences }
    val loadImages = remember { preferences.imagesInDescription.get() }
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    val scrimHeight = KatariTheme.spacing.xl
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
                    text = if (notes.isBlank()) "\n\n" else "\n\n\n\n\n",
                    role = TextRole.Body,
                )
            },
            {
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        infoHeight = size.height
                    },
                ) {
                    EntryNotesSection(
                        content = notes,
                        expanded = expanded,
                        onEditNotes = onEditNotesClicked,
                    )
                    SelectionContainer {
                        MarkdownRender(
                            content = description,
                            annotator = descriptionAnnotator(
                                loadImages = loadImages,
                                linkStyle = markdownStyle().link,
                            ),
                            loadImages = loadImages,
                            secondary = true,
                        )
                    }
                }
            },
            {
                Box(
                    modifier = Modifier.fadeIntoBackground(),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_caret_flip)
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        modifier = Modifier.backgroundHalo(),
                    )
                }
            },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeightPx = scrimHeight.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeightPx))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeightPx) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeightPx
            scrimPlaceable.place(0, scrimY)
        }
    }
}
