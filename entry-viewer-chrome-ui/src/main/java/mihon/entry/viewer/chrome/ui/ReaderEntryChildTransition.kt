package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.MarkerLine
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.surface.ContentOn
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

data class ReaderEntryChildTransitionItem(
    val name: String,
    val subtitle: String? = null,
    val availableOffline: Boolean = false,
)

sealed interface ReaderEntryChildTransitionLoadState {
    data object Idle : ReaderEntryChildTransitionLoadState

    data class Loading(val message: String) : ReaderEntryChildTransitionLoadState

    data class Failed(val message: String) : ReaderEntryChildTransitionLoadState
}

enum class ReaderEntryChildTransitionDestinationSlot {
    TOP,
    BOTTOM,
}

/** Presentation-only transition model shared by built-in entry viewers. */
data class ReaderEntryChildTransitionUiModel(
    val topLabel: String,
    val topChild: ReaderEntryChildTransitionItem?,
    val bottomLabel: String,
    val bottomChild: ReaderEntryChildTransitionItem?,
    val fallbackLabel: String,
    val missingChildCount: Int = 0,
    val destinationLoadState: ReaderEntryChildTransitionLoadState,
    val destinationSlot: ReaderEntryChildTransitionDestinationSlot,
)

/**
 * Format-neutral boundary content for manga chapters, BOOK children, and episodes. It takes its colours from the
 * surrounding theme, so a reader that themes its pages with ReadingPaletteTheme gets a boundary matching them.
 */
@Composable
fun ReaderEntryChildTransition(
    model: ReaderEntryChildTransitionUiModel,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ContentOn(SurfaceContainer.Surface) {
        Column(
            modifier = modifier
                .widthIn(max = KatariTheme.sizes.readableWidth)
                .fillMaxWidth()
                .padding(vertical = KatariTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            model.topChild?.let { child ->
                TransitionChild(header = model.topLabel, child = child, prominent = false)
            } ?: NoChildNotification(text = model.fallbackLabel)
            if (model.destinationSlot == ReaderEntryChildTransitionDestinationSlot.TOP) {
                DestinationLoadStatus(state = model.destinationLoadState, onRetry = onRetry)
            }

            model.bottomChild?.let { child ->
                if (model.missingChildCount > 0) {
                    ChildGapWarning(gapCount = model.missingChildCount)
                } else {
                    MarkerLine(text = null)
                }
                TransitionChild(header = model.bottomLabel, child = child, prominent = true)
            } ?: NoChildNotification(text = model.fallbackLabel)

            if (model.destinationSlot == ReaderEntryChildTransitionDestinationSlot.BOTTOM) {
                DestinationLoadStatus(state = model.destinationLoadState, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun DestinationLoadStatus(
    state: ReaderEntryChildTransitionLoadState,
    onRetry: (() -> Unit)?,
) {
    when (state) {
        ReaderEntryChildTransitionLoadState.Idle -> Unit
        is ReaderEntryChildTransitionLoadState.Loading -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
                Text(state.message, role = TextRole.Body)
            }
        }
        is ReaderEntryChildTransitionLoadState.Failed -> {
            OutlinedNotice {
                Column(
                    modifier = Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m),
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(MaterialSymbols.RoundedFilled.Warning, contentDescription = null, tone = ContentTone.Error)
                        Text(state.message, role = TextRole.Body)
                    }
                    onRetry?.let { retry ->
                        Button(
                            label = stringResource(MR.strings.action_retry),
                            onClick = retry,
                            style = ButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoChildNotification(text: String) {
    OutlinedNoticeRow {
        Icon(MaterialSymbols.Rounded.Info, contentDescription = null, tone = ContentTone.Primary)
        Text(text = text, role = TextRole.Body)
    }
}

@Composable
private fun ChildGapWarning(gapCount: Int) {
    OutlinedNoticeRow {
        Icon(MaterialSymbols.RoundedFilled.Warning, contentDescription = null, tone = ContentTone.Error)
        Text(
            text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
            role = TextRole.Body,
        )
    }
}

/** An outline without a fill, so the notice sits on whatever background the reader gives its pages. */
@Composable
private fun OutlinedNotice(content: @Composable () -> Unit) {
    Card(style = CardStyle.Outlined, container = SurfaceContainer.None) { content() }
}

@Composable
private fun OutlinedNoticeRow(content: @Composable RowScope.() -> Unit) {
    OutlinedNotice {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun TransitionChild(
    header: String,
    child: ReaderEntryChildTransitionItem,
    prominent: Boolean,
) {
    val iconSize = with(LocalDensity.current) {
        (if (prominent) KatariTheme.sizes.icon else KatariTheme.sizes.iconSmall).toSp()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = header,
            modifier = Modifier.padding(bottom = KatariTheme.spacing.xxs),
            role = TextRole.Label,
            tone = ContentTone.Secondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildAnnotatedString {
                if (child.availableOffline) {
                    appendInlineContent(AVAILABLE_OFFLINE_ICON_ID)
                    append(' ')
                }
                append(child.name)
            },
            role = if (prominent) TextRole.ScreenTitle else TextRole.ItemTitle,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            inlineContent = mapOf(
                AVAILABLE_OFFLINE_ICON_ID to InlineTextContent(
                    Placeholder(
                        width = iconSize,
                        height = iconSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                        contentDescription = stringResource(MR.strings.label_downloaded),
                    )
                },
            ),
        )
        child.subtitle?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = KatariTheme.spacing.xxs),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val AVAILABLE_OFFLINE_ICON_ID = "available_offline"
