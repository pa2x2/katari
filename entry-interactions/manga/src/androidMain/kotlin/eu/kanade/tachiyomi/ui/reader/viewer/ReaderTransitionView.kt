package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import eu.kanade.presentation.reader.ChapterTransitionPlacement
import eu.kanade.presentation.reader.ViewerChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.runtime.EntryInteractionTheme
import mihon.entry.interactions.viewer.EntryChildTransition

internal class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AbstractComposeView(context, attrs) {

    private var data: Data? by mutableStateOf(null)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    internal fun bind(
        transition: EntryChildTransition<ReaderChapter>,
        downloadManager: DownloadManager,
        config: ViewerConfig,
        placement: ChapterTransitionPlacement,
        onRetry: (ReaderChapter) -> Unit,
    ) {
        data = Data(transition, downloadManager, config, placement, onRetry)
    }

    @Composable
    override fun Content() {
        data?.let {
            EntryInteractionTheme {
                ViewerChapterTransition(
                    transition = it.transition,
                    downloadManager = it.downloadManager,
                    displayMode = it.config.chapterTransitionMode,
                    placement = it.placement,
                    onRetry = it.onRetry,
                )
            }
        }
    }

    private data class Data(
        val transition: EntryChildTransition<ReaderChapter>,
        val downloadManager: DownloadManager,
        val config: ViewerConfig,
        val placement: ChapterTransitionPlacement,
        val onRetry: (ReaderChapter) -> Unit,
    )
}
