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
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerHeadingText
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSourcePicker
import eu.kanade.tachiyomi.ui.video.player.components.VideoSourceOptions
import eu.kanade.tachiyomi.ui.video.player.recovery.sourceChoice
import mihon.entry.interactions.source.EntryChildWebViewAction

/**
 * An episode that couldn't open: its problem screen, and the picker for the dubs, servers, streams and qualities the
 * source listed before it failed, which opens the episode again with the one picked.
 */
@Composable
internal fun VideoPlayerStartupProblem(
    current: VideoPlayerViewModel.State.Error,
    viewModel: VideoPlayerViewModel,
    host: VideoPlayerScreenHost,
) {
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    val choices = current.choices
    VideoPlayerErrorOverlay(
        problem = current.problem,
        onBack = host.onClose,
        onRetry = viewModel::retry,
        onOpenWebView = current.webView?.let { webView ->
            {
                host.onOpenWebView(
                    EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                    webView,
                    current.title.orEmpty(),
                )
            }
        },
        sourceChoice = choices?.sourceChoice()?.let { SourceChoiceAction(it) { pickerVisible = true } },
        heading = current.title?.let { VideoPlayerHeadingText(it, current.episodeName.orEmpty()) },
        failedServers = choices?.servers.orEmpty().filter { it.key in current.failedServerKeys }.map { it.label },
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
