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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.layout.PanelBesideMedia
import app.katari.presentation.kit.surface.MediaScrim
import app.katari.presentation.kit.surface.MediaScrimEdge
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.stableNavigationBars
import app.katari.presentation.kit.window.stableStatusBars

/**
 * The immersive view's controls over one title: the feed's pill at the top, and the title with its actions in a bar
 * along the bottom, both shown while [visible]. Where the media, [mediaWidth] wide, leaves room beside it, the title
 * and actions sit in a panel there instead, always shown. [titleSteps] are buttons to the previous and next title, at
 * the media's end edge.
 */
@Composable
internal fun ImmersiveOverlay(
    visible: Boolean,
    contextLabel: String,
    contextSupportingLabel: String?,
    contextLeadingContent: (@Composable () -> Unit)?,
    onContextClick: (() -> Unit)?,
    view: ImmersiveViewActions,
    entry: ImmersiveEntryActions,
    mediaWidth: Dp?,
    onBottomOverlaySize: (Int) -> Unit,
    titleSteps: ImmersiveTitleSteps?,
) {
    PanelBesideMedia(
        mediaWidth = mediaWidth,
        modifier = Modifier.fillMaxSize(),
        panel = { ImmersiveSidePanel(view = view, entry = entry) },
    ) { panelRoom ->
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

            if (panelRoom == null) {
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
                        ImmersiveBottomBar(view = view, entry = entry)
                    }
                }
            } else {
                // The media's own controls, such as a page slider, go to the bottom edge with no bar over them
                LaunchedEffect(Unit) { onBottomOverlaySize(0) }
            }

            if (titleSteps != null) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = (panelRoom ?: KatariTheme.sizes.none) + KatariTheme.spacing.m),
                ) {
                    ImmersiveTitleStepButtons(titleSteps)
                }
            }
        }
    }
}

@Composable
private fun ImmersiveBottomBar(view: ImmersiveViewActions, entry: ImmersiveEntryActions) {
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
                text = entry.entry.displayTitle,
                role = TextRole.SectionTitle,
                tone = ContentTone.OnMedia,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.chapterName?.let {
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
            ImmersiveViewActionButtons(view)
            ImmersiveEntryActionButtons(entry)
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
