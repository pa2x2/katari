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
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FullscreenExit
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

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
    onOpenChapter: (() -> Unit)?,
    onBottomOverlaySize: (Int) -> Unit,
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
                            top = WindowInsets.statusBarsIgnoringVisibility
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
                    onOpenChapter = onOpenChapter,
                )
            }
        }
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
    onOpenChapter: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = KatariTheme.spacing.l,
                top = KatariTheme.spacing.xxl,
                end = KatariTheme.spacing.l,
                bottom = WindowInsets.navigationBarsIgnoringVisibility
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
            if (showBackToTop) {
                ImmersiveActionButton(
                    label = stringResource(MR.strings.action_move_to_top),
                    icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                    onClick = onBackToTop,
                )
            }
            ImmersiveActionButton(
                label = stringResource(MR.strings.browse_exit_immersive),
                icon = MaterialSymbols.Rounded.FullscreenExit,
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
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            container = SurfaceContainer.MediaScrim,
            shape = KatariTheme.shapes.full,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            container = SurfaceContainer.MediaScrim,
            shape = KatariTheme.shapes.full,
            content = content,
        )
    }
}

// The bottom scrim already gives these buttons their contrast, so they are bare icons in the media content colour.
@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(icon = icon, contentDescription = label, onClick = onClick, enabled = enabled)
}
