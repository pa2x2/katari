package mihon.entry.interactions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryImmersiveFeedInteraction {
    fun isSupported(entry: Entry): Boolean

    fun preloadRadius(entryType: EntryType): Int

    suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        source: UnifiedSource,
    ): EntryImmersiveFeedHandle

    fun renderer(handle: EntryImmersiveFeedHandle): EntryImmersiveFeedRenderer

    suspend fun persistProgress(handle: EntryImmersiveFeedHandle, progress: EntryImmersiveFeedProgress)

    fun release(handle: EntryImmersiveFeedHandle)
}

fun interface EntryImmersiveFeedRenderer {
    @Composable
    fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onZoomStateChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveFeedProgress) -> Unit,
    )
}

sealed interface EntryImmersiveFeedProgress {
    data class ImagePage(
        val pageIndex: Int,
        val pageCount: Int,
        val sessionDurationMs: Long,
    ) : EntryImmersiveFeedProgress

    data class Playback(
        val positionMs: Long,
        val durationMs: Long,
        val resetSession: Boolean = false,
    ) : EntryImmersiveFeedProgress
}

sealed interface EntryImmersiveFeedHandle {
    val entryType: EntryType
    val chapterId: Long

    data class ImagePages(
        override val entryType: EntryType,
        override val chapterId: Long,
        val delegate: Any,
    ) : EntryImmersiveFeedHandle

    data class Playback(
        override val entryType: EntryType,
        override val chapterId: Long,
        val stream: VideoStream,
        val subtitles: List<VideoSubtitle>,
        val resumePositionMs: Long,
        val delegate: Any? = null,
    ) : EntryImmersiveFeedHandle
}
