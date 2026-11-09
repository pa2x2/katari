package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.Fullscreen
import app.katari.icons.materialsymbols.rounded.FullscreenExit
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.button.IconButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.refresh.RefreshButton
import app.katari.presentation.kit.surface.MediaScrim
import app.katari.presentation.kit.surface.MediaScrimEdge
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.WindowFullScreen
import app.katari.presentation.kit.window.stableNavigationBars
import app.katari.presentation.kit.window.stableStatusBars
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The previous and next title buttons: [hasPrevious] and [hasNext] say whether there is a title that way. */
internal class ImmersiveTitleSteps(
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

@Composable
internal fun ImmersiveOverlay(
    visible: Boolean,
    entry: Entry,
    chapterName: String?,
    contextLabel: String,
    contextSupportingLabel: String?,
    contextLeadingContent: (@Composable () -> Unit)?,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: () -> Unit,
    onLibraryAction: () -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    onOpenChapter: (() -> Unit)?,
    onBottomOverlaySize: (Int) -> Unit,
    fullScreen: WindowFullScreen?,
    /** Buttons to the previous and next title, where the mouse has no swipe to move between them; null for none. */
    titleSteps: ImmersiveTitleSteps?,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / 5 },
            exit = fadeOut() + slideOutVertically { -it / 5 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            MediaScrim(edge = MediaScrimEdge.Top, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = KatariTheme.spacing.m,
                            top = WindowInsets.stableStatusBars
                                .asPaddingValues()
                                .calculateTopPadding() + KatariTheme.spacing.m,
                            end = KatariTheme.spacing.m,
                            bottom = KatariTheme.spacing.xxl,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ImmersiveContextPill(
                        label = contextLabel,
                        supportingLabel = contextSupportingLabel,
                        leadingContent = contextLeadingContent,
                        onClick = onContextClick,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 5 },
            exit = fadeOut() + slideOutVertically { it / 5 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MediaScrim(
                edge = MediaScrimEdge.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onBottomOverlaySize(it.height) },
            ) {
                ImmersiveBottomBar(
                    entry = entry,
                    chapterName = chapterName,
                    onExitImmersive = onExitImmersive,
                    onEntryClick = onEntryClick,
                    onLibraryAction = onLibraryAction,
                    showBackToTop = showBackToTop,
                    onBackToTop = onBackToTop,
                    onRefresh = onRefresh,
                    refreshEnabled = refreshEnabled,
                    onOpenChapter = onOpenChapter,
                    fullScreen = fullScreen,
                )
            }
        }

        if (titleSteps != null) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = KatariTheme.spacing.m),
            ) {
                ImmersiveTitleStepButtons(titleSteps)
            }
        }
    }
}

@Composable
private fun ImmersiveTitleStepButtons(steps: ImmersiveTitleSteps) {
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
        IconButton(
            icon = MaterialSymbols.Rounded.KeyboardArrowUp,
            contentDescription = stringResource(MR.strings.browse_immersive_previous),
            onClick = steps.onPrevious,
            modifier = Modifier.mediaControl(),
            style = IconButtonStyle.Tonal,
            size = IconButtonSize.Large,
            enabled = steps.hasPrevious,
            shortcut = previousTitleShortcut(),
        )
        IconButton(
            icon = MaterialSymbols.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(MR.strings.browse_immersive_next),
            onClick = steps.onNext,
            modifier = Modifier.mediaControl(),
            style = IconButtonStyle.Tonal,
            size = IconButtonSize.Large,
            enabled = steps.hasNext,
            shortcut = nextTitleShortcut(),
        )
    }
}

@Composable
private fun ImmersiveBottomBar(
    entry: Entry,
    chapterName: String?,
    onExitImmersive: () -> Unit,
    onEntryClick: () -> Unit,
    onLibraryAction: () -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    onOpenChapter: (() -> Unit)?,
    fullScreen: WindowFullScreen?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = KatariTheme.spacing.l,
                top = KatariTheme.spacing.xxl,
                end = KatariTheme.spacing.l,
                bottom = WindowInsets.stableNavigationBars
                    .asPaddingValues()
                    .calculateBottomPadding() + KatariTheme.spacing.s,
            ),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        ) {
            Text(
                text = entry.displayTitle,
                role = TextRole.SectionTitle,
                tone = ContentTone.OnMedia,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            chapterName?.let {
                Text(
                    text = it,
                    role = TextRole.Supporting,
                    tone = ContentTone.OnMediaVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RefreshButton(onClick = onRefresh, modifier = Modifier.mediaControl(), enabled = refreshEnabled)
            if (showBackToTop) {
                ImmersiveActionButton(
                    label = stringResource(MR.strings.action_move_to_top),
                    icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                    onClick = onBackToTop,
                )
            }
            if (fullScreen != null) {
                ImmersiveActionButton(
                    label = stringResource(
                        if (fullScreen.isFullScreen) {
                            MR.strings.action_exit_full_screen
                        } else {
                            MR.strings.action_enter_full_screen
                        },
                    ),
                    icon = if (fullScreen.isFullScreen) {
                        MaterialSymbols.Rounded.FullscreenExit
                    } else {
                        MaterialSymbols.Rounded.Fullscreen
                    },
                    onClick = fullScreen::toggle,
                )
            }
            ImmersiveActionButton(
                label = stringResource(MR.strings.browse_exit_immersive),
                icon = MaterialSymbols.Rounded.Close,
                onClick = onExitImmersive,
            )
            ImmersiveActionButton(
                label = stringResource(
                    if (entry.favorite) MR.strings.remove_from_library else MR.strings.add_to_library,
                ),
                icon = EntryActionIcons.library(entry.favorite),
                onClick = onLibraryAction,
            )
            ImmersiveActionButton(
                label = stringResource(MR.strings.action_details),
                icon = MaterialSymbols.Rounded.Info,
                onClick = onEntryClick,
            )
            val presentation = entry.type.entryTypePresentation()
            ImmersiveActionButton(
                label = stringResource(presentation.immersiveOpenLabel),
                icon = presentation.immersiveOpenIcon,
                onClick = { onOpenChapter?.invoke() },
                enabled = onOpenChapter != null,
            )
        }
    }
}

@Composable
private fun ImmersiveContextPill(
    label: String,
    supportingLabel: String?,
    leadingContent: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent?.invoke()
            Column {
                Text(
                    text = label,
                    role = TextRole.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                supportingLabel?.let {
                    Text(
                        text = it,
                        role = TextRole.Badge,
                        tone = ContentTone.OnMediaVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    OverlayPill(modifier = modifier.mediaControl(), style = OverlayStyle.Scrim, onClick = onClick, content = content)
}

// The bottom scrim already gives these buttons their contrast, so they are bare icons in the media content colour.
@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        modifier = Modifier.mediaControl(),
        enabled = enabled,
    )
}
