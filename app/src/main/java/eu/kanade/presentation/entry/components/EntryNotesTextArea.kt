package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import eu.kanade.presentation.entry.EntryNotesScreen
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.FormatListBulleted
import mihon.icons.materialsymbols.rounded.FormatBold
import mihon.icons.materialsymbols.rounded.FormatItalic
import mihon.icons.materialsymbols.rounded.FormatListNumbered
import mihon.icons.materialsymbols.rounded.FormatUnderlined
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.documentSpanStyle
import mihon.presentation.kit.field.RichTextField
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.VerticalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun EntryNotesTextArea(
    state: EntryNotesScreen.State,
    onUpdate: (String) -> Unit,
    onFinish: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val richTextState = rememberRichTextState()
    val primaryColor = KatariTheme.colors.primary

    DisposableEffect(scope, richTextState) {
        snapshotFlow { richTextState.annotatedString }
            .debounce(0.25.seconds)
            .distinctUntilChanged()
            .map { richTextState.toMarkdown() }
            .onEach { onUpdate(it) }
            .launchIn(scope)

        onDispose {
            onFinish(richTextState.toMarkdown())
        }
    }
    LaunchedEffect(Unit) {
        richTextState.setMarkdown(state.notes)
        richTextState.config.unorderedListIndent = 4
        richTextState.config.orderedListIndent = 20
    }
    LaunchedEffect(primaryColor) {
        richTextState.config.linkColor = primaryColor
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    val textLength = remember(richTextState.annotatedString) { richTextState.toText().length }

    Column(
        modifier = modifier
            .padding(horizontal = KatariTheme.spacing.s)
            .fillMaxSize(),
    ) {
        RichTextField(
            state = richTextState,
            placeholder = stringResource(MR.strings.notes_placeholder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(vertical = KatariTheme.spacing.s)
                .fillMaxWidth(),
        ) {
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
            ) {
                item {
                    EntryNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(BoldSpan) },
                        isSelected = richTextState.currentSpanStyle.includes(BoldSpan),
                        icon = MaterialSymbols.Rounded.FormatBold,
                    )
                }
                item {
                    EntryNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(ItalicSpan) },
                        isSelected = richTextState.currentSpanStyle.includes(ItalicSpan),
                        icon = MaterialSymbols.Rounded.FormatItalic,
                    )
                }
                item {
                    EntryNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(UnderlineSpan) },
                        isSelected = richTextState.currentSpanStyle.textDecoration
                            ?.contains(TextDecoration.Underline)
                            ?: false,
                        icon = MaterialSymbols.Rounded.FormatUnderlined,
                    )
                }
                item {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = KatariTheme.spacing.xs)
                            .height(KatariTheme.spacing.l),
                    )
                }
                item {
                    EntryNotesTextAreaButton(
                        onClick = { richTextState.toggleUnorderedList() },
                        isSelected = richTextState.isUnorderedList,
                        icon = MaterialSymbols.AutoMirroredRounded.FormatListBulleted,
                    )
                }
                item {
                    EntryNotesTextAreaButton(
                        onClick = { richTextState.toggleOrderedList() },
                        isSelected = richTextState.isOrderedList,
                        icon = MaterialSymbols.Rounded.FormatListNumbered,
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = textLength.toString(),
                    modifier = Modifier.padding(KatariTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
fun EntryNotesTextAreaButton(
    onClick: () -> Unit,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = if (isSelected) SurfaceContainer.Inverse else SurfaceContainer.None,
        shape = KatariTheme.shapes.small,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            modifier = Modifier.padding(KatariTheme.spacing.xs),
            tone = if (isSelected) ContentTone.Inherit else ContentTone.Primary,
        )
    }
}

/** Whether the style already applies [toggle], so adding it would change nothing. */
private fun SpanStyle.includes(toggle: SpanStyle): Boolean = merge(toggle) == this

private val BoldSpan = documentSpanStyle(bold = true)
private val ItalicSpan = documentSpanStyle(italic = true)
private val UnderlineSpan = documentSpanStyle(textDecoration = TextDecoration.Underline)
