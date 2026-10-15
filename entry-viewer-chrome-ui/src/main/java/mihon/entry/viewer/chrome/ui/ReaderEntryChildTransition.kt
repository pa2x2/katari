package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
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
 * Format-neutral boundary content for manga chapters, BOOK children, and episodes. The colours default to the app
 * theme; a reader with its own page palette passes that palette so the boundary matches the pages around it.
 */
@Composable
fun ReaderEntryChildTransition(
    model: ReaderEntryChildTransitionUiModel,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentColor: Color = KatariTheme.colors.onSurface,
    secondaryContentColor: Color = KatariTheme.colors.onSurfaceVariant,
    accentColor: Color = KatariTheme.colors.primary,
    warningColor: Color = KatariTheme.colors.error,
    outlineColor: Color = KatariTheme.colors.outlineVariant,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = modifier
                .widthIn(max = KatariTheme.sizes.readableWidth)
                .fillMaxWidth()
                .padding(vertical = KatariTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            model.topChild?.let { child ->
                TransitionChild(
                    header = model.topLabel,
                    child = child,
                    prominent = false,
                    secondaryContentColor = secondaryContentColor,
                )
            } ?: NoChildNotification(
                text = model.fallbackLabel,
                accentColor = accentColor,
                outlineColor = outlineColor,
            )
            if (model.destinationSlot == ReaderEntryChildTransitionDestinationSlot.TOP) {
                DestinationLoadStatus(
                    state = model.destinationLoadState,
                    onRetry = onRetry,
                    accentColor = accentColor,
                    warningColor = warningColor,
                    outlineColor = outlineColor,
                )
            }

            model.bottomChild?.let { child ->
                if (model.missingChildCount > 0) {
                    ChildGapWarning(
                        gapCount = model.missingChildCount,
                        warningColor = warningColor,
                        outlineColor = outlineColor,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.32f)
                            .height(KatariTheme.sizes.border)
                            .background(outlineColor),
                    )
                }
                TransitionChild(
                    header = model.bottomLabel,
                    child = child,
                    prominent = true,
                    secondaryContentColor = secondaryContentColor,
                )
            } ?: NoChildNotification(
                text = model.fallbackLabel,
                accentColor = accentColor,
                outlineColor = outlineColor,
            )

            if (model.destinationSlot == ReaderEntryChildTransitionDestinationSlot.BOTTOM) {
                DestinationLoadStatus(
                    state = model.destinationLoadState,
                    onRetry = onRetry,
                    accentColor = accentColor,
                    warningColor = warningColor,
                    outlineColor = outlineColor,
                )
            }
        }
    }
}

@Composable
private fun DestinationLoadStatus(
    state: ReaderEntryChildTransitionLoadState,
    onRetry: (() -> Unit)?,
    accentColor: Color,
    warningColor: Color,
    outlineColor: Color,
) {
    when (state) {
        ReaderEntryChildTransitionLoadState.Idle -> Unit
        is ReaderEntryChildTransitionLoadState.Loading -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(KatariTheme.sizes.icon),
                    color = accentColor,
                )
                Text(state.message, role = TextRole.Body)
            }
        }
        is ReaderEntryChildTransitionLoadState.Failed -> {
            Column(
                modifier = Modifier
                    .border(KatariTheme.sizes.border, outlineColor, KatariTheme.shapes.medium)
                    .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TintedIcon(MaterialSymbols.RoundedFilled.Warning, warningColor)
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

@Composable
private fun NoChildNotification(
    text: String,
    accentColor: Color,
    outlineColor: Color,
) {
    OutlinedNotice(outlineColor = outlineColor) {
        TintedIcon(MaterialSymbols.Rounded.Info, accentColor)
        Text(text = text, role = TextRole.Body)
    }
}

@Composable
private fun ChildGapWarning(
    gapCount: Int,
    warningColor: Color,
    outlineColor: Color,
) {
    OutlinedNotice(outlineColor = outlineColor) {
        TintedIcon(MaterialSymbols.RoundedFilled.Warning, warningColor)
        Text(
            text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
            role = TextRole.Body,
        )
    }
}

@Composable
private fun OutlinedNotice(
    outlineColor: Color,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .border(KatariTheme.sizes.border, outlineColor, KatariTheme.shapes.medium)
            .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/** An icon in one of the palette colours this transition is given, which need not be theme roles. */
@Composable
private fun TintedIcon(icon: ImageVector, color: Color) {
    CompositionLocalProvider(LocalContentColor provides color) {
        Icon(imageVector = icon, contentDescription = null)
    }
}

@Composable
private fun TransitionChild(
    header: String,
    child: ReaderEntryChildTransitionItem,
    prominent: Boolean,
    secondaryContentColor: Color,
) {
    val iconSize = with(LocalDensity.current) {
        (if (prominent) KatariTheme.sizes.icon else KatariTheme.sizes.iconSmall).toSp()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CompositionLocalProvider(LocalContentColor provides secondaryContentColor) {
            Text(
                text = header,
                modifier = Modifier.padding(bottom = KatariTheme.spacing.xxs),
                role = TextRole.Label,
                textAlign = TextAlign.Center,
            )
        }
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
            CompositionLocalProvider(LocalContentColor provides secondaryContentColor) {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = KatariTheme.spacing.xxs),
                    role = TextRole.Body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val AVAILABLE_OFFLINE_ICON_ID = "available_offline"
