package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.SourceChoiceAction
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSourcePicker
import eu.kanade.tachiyomi.ui.video.player.components.VideoSourceOptions
import eu.kanade.tachiyomi.ui.video.player.recovery.sourceChoice

/**
 * The episode the player opens, before it has a stream: the wait, and the picker for the dubs, servers, streams and
 * qualities the source listed, which opens it with the one picked instead.
 */
@Composable
internal fun VideoPlayerStartupLoading(
    current: VideoPlayerViewModel.State.Loading,
    viewModel: VideoPlayerViewModel,
    host: VideoPlayerScreenHost,
) {
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    val choices = current.choices
    VideoPlayerLoadingOverlay(
        videoTitle = current.title,
        episodeName = current.episodeName,
        onBack = host.onClose,
        step = current.step,
        sourceChoice = choices?.sourceChoice()?.let { SourceChoiceAction(it) { pickerVisible = true } },
        modifier = Modifier.fillMaxSize(),
    )
    if (choices != null) {
        VideoPlayerSourcePicker(
            visible = pickerVisible,
            options = VideoSourceOptions(
                listed = choices,
                current = choices.selection,
                active = choices.selection,
                preview = current.preview,
                failedServerKeys = current.failedServerKeys,
            ),
            onPreview = viewModel::previewSourceSelection,
            onApply = viewModel::openWithSelection,
            onDismissRequest = { pickerVisible = false },
        )
    }
}
