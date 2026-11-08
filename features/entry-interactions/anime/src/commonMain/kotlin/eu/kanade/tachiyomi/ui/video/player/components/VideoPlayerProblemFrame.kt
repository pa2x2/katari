package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.ArrowBack
import app.katari.icons.materialsymbols.rounded.ContentCopy
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.ContentButton
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.surface.background
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The player's problem screen over the video: what went wrong in words, optional [progress] under it, the [actions]
 * that may fix it, and the technical [details] folded away for a bug report. Back stays where the top bar has it,
 * with the [heading] the top bar shows when there is one.
 */
@Composable
internal fun VideoPlayerProblemFrame(
    title: String,
    explanation: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    heading: VideoPlayerHeadingText? = null,
    details: String? = null,
    progress: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    Box(modifier = modifier.background(SurfaceContainer.Media)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = KatariTheme.sizes.readableWidth)
                .verticalScroll(rememberScrollState())
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true)
                .padding(horizontal = spacing.xl, vertical = spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Text(
                text = title,
                role = TextRole.SheetTitle,
                tone = ContentTone.OnMedia,
                textAlign = TextAlign.Center,
            )
            Text(
                text = explanation,
                role = TextRole.Body,
                tone = ContentTone.OnMediaVariant,
                textAlign = TextAlign.Center,
            )
            progress?.invoke(this)
            FlowRow(
                modifier = Modifier.padding(top = spacing.s),
                horizontalArrangement = Arrangement.spacedBy(spacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
                content = actions,
            )
            details?.let { ProblemDetails(details = it) }
        }
        if (heading != null) {
            VideoPlayerHeading(
                videoTitle = heading.videoTitle,
                episodeName = heading.episodeName,
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .videoPlayerSafeContentPadding(includeTop = true)
                    .padding(spacing.s),
            )
        } else {
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                contentDescription = stringResource(MR.strings.action_back),
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .videoPlayerSafeContentPadding(includeTop = true)
                    .padding(spacing.s),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
    }
}

@Composable
private fun ProblemDetails(details: String) {
    val spacing = KatariTheme.spacing
    val clipboard = LocalVideoPlayerClipboard.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(MR.strings.anime_playback_details)
    Column(modifier = Modifier.fillMaxWidth().padding(top = spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentButton(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(vertical = spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            MaterialSymbols.Rounded.KeyboardArrowUp
                        } else {
                            MaterialSymbols.Rounded.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tone = ContentTone.OnMediaVariant,
                    )
                    Text(text = title, role = TextRole.Label, tone = ContentTone.OnMediaVariant)
                }
            }
            IconButton(
                icon = MaterialSymbols.Rounded.ContentCopy,
                contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                onClick = { clipboard.copy(label = title, text = details) },
                tone = ContentTone.OnMediaVariant,
            )
        }
        if (expanded) {
            Text(text = details, role = TextRole.Supporting, tone = ContentTone.OnMediaVariant)
        }
    }
}

/** Where the problem screens copy their details, which the player screen provides from its host. */
internal val LocalVideoPlayerClipboard = staticCompositionLocalOf<TextClipboard> {
    error("The player screen provides the clipboard")
}
