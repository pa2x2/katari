package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ImmersiveOverlay(
    visible: Boolean,
    entry: Entry,
    chapterName: String?,
    contextLabel: String,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                        ),
                    )
                    .padding(
                        start = 12.dp,
                        top = WindowInsets.statusBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateTopPadding() + 12.dp,
                        end = 12.dp,
                        bottom = 36.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImmersiveContextPill(
                    label = contextLabel,
                    leadingContent = contextLeadingContent,
                    onClick = onContextClick,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 5 },
            exit = fadeOut() + slideOutVertically { it / 5 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onBottomOverlaySize(it.height) }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(
                        start = 16.dp,
                        top = 40.dp,
                        end = 16.dp,
                        bottom = WindowInsets.navigationBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateBottomPadding() + 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = entry.displayTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    chapterName?.let {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackToTop) {
                        ImmersiveActionButton(
                            label = stringResource(MR.strings.action_move_to_top),
                            icon = Icons.Outlined.KeyboardArrowUp,
                            onClick = onBackToTop,
                        )
                    }
                    ImmersiveActionButton(
                        label = stringResource(MR.strings.browse_exit_immersive),
                        icon = Icons.Outlined.FullscreenExit,
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
                        icon = Icons.Outlined.Info,
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
    }
}

@Composable
private fun ImmersiveContextPill(
    label: String,
    leadingContent: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            contentColor = Color.White,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            contentColor = Color.White,
            content = content,
        )
    }
}

@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(21.dp))
        }
    }
}
