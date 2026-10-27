package eu.kanade.tachiyomi.ui.video.player.components

import android.content.ClipData
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
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
                onClick = {
                    scope.launch { clipboard.setClipEntry(ClipData.newPlainText(title, details).toClipEntry()) }
                },
                tone = ContentTone.OnMediaVariant,
            )
        }
        if (expanded) {
            Text(text = details, role = TextRole.Supporting, tone = ContentTone.OnMediaVariant)
        }
    }
}
