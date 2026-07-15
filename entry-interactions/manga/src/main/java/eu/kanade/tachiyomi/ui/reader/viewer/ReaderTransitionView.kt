package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import eu.kanade.presentation.reader.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.EntryInteractionTheme
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.viewer.EntryChildTransition

internal class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AbstractComposeView(context, attrs) {

    private var data: Data? by mutableStateOf(null)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    internal fun bind(transition: EntryChildTransition<ReaderChapter>, downloadManager: DownloadManager) {
        val toChapter = transition.to
        val toManga = toChapter?.manga

        data = Data(
            transition = transition,
            currChapterDownloaded = transition.from.pageLoader?.isLocal == true,
            goingToChapterDownloaded =
            if (toManga == null) {
                false
            } else {
                toManga.source == tachiyomi.source.local.LocalSource.ID ||
                    downloadManager.isChapterDownloaded(
                        chapterName = toChapter.chapter.name,
                        chapterScanlator = toChapter.chapter.scanlator,
                        chapterUrl = toChapter.chapter.url,
                        mangaTitle = toManga.title,
                        sourceId = toManga.source,
                        skipCache = true,
                    )
            },
        )
    }

    @Composable
    override fun Content() {
        data?.let {
            EntryInteractionTheme {
                ChapterTransition(
                    transition = it.transition,
                    currChapterDownloaded = it.currChapterDownloaded,
                    goingToChapterDownloaded = it.goingToChapterDownloaded,
                )
            }
        }
    }

    private data class Data(
        val transition: EntryChildTransition<ReaderChapter>,
        val currChapterDownloaded: Boolean,
        val goingToChapterDownloaded: Boolean,
    )
}
