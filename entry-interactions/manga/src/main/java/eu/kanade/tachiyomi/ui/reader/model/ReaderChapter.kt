package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.domain.chapter.model.toDbChapter
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import kotlinx.coroutines.flow.MutableStateFlow
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry

internal data class ReaderChapter(
    val chapter: Chapter,
    val manga: Entry? = null,
    /** Null when the chapter is in none of its entry's groups, or the entry has none. */
    val group: ReaderChapterGroup? = null,
) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    var firstPagesPreloaded: Boolean = false

    private var references = 0

    constructor(
        chapter: tachiyomi.domain.chapter.model.Chapter,
        manga: Entry? = null,
        group: ReaderChapterGroup? = null,
    ) : this(chapter.toDbChapter(), manga, group)

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            if (pageLoader != null) {
                logcat { "Recycling chapter ${chapter.name}" }
            }
            pageLoader?.recycle()
            pageLoader = null
            firstPagesPreloaded = false
            state = State.Wait
        }
    }

    sealed interface State {
        data object Wait : State
        data object Loading : State
        data class Error(val error: Throwable) : State
        data class Loaded(val pages: List<ReaderPage>) : State
    }
}
