package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.theme.KatariTheme

/**
 * The settings with only the source rows, for an episode that isn't playing: one that failed to open, or is still
 * opening. Apply plays it with the drafted selection.
 */
@Composable
internal fun VideoPlayerSourcePicker(
    visible: Boolean,
    options: VideoSourceOptions,
    onPreview: (PlaybackSelection) -> Unit,
    onApply: (PlaybackSelection) -> Unit,
    onDismissRequest: () -> Unit,
) {
    VideoPlayerSettings(visible = visible, onDismissRequest = onDismissRequest) { frame, modifier ->
        var draft by remember(options.active) { mutableStateOf(options.active) }
        val rows = videoSourceRows(options, draft)
        VideoSourcePreviewEffect(options, draft, onPreview)
        Column(modifier = modifier) {
            VideoPlayerSettingsHeader(frame = frame, onDismissRequest = onDismissRequest)
            RowList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
                contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
            ) {
                videoSourceRowItems(
                    rows = rows,
                    draft = draft,
                    options = options,
                    onDraft = { draft = it },
                    onRetryPreview = { onPreview(draft) },
                    onCancel = { draft = options.active },
                    onApply = {
                        onApply(draft)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
