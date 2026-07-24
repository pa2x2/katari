package mihon.entry.interactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

fun interface EntryImmersiveRenderer {
    @Composable
    fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onZoomStateChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveProgress) -> Unit,
    )
}

sealed interface EntryImmersiveProgress {
    data class ImagePage(
        val pageIndex: Int,
        val pageCount: Int,
        val sessionDurationMs: Long,
    ) : EntryImmersiveProgress

    data class Playback(
        val positionMs: Long,
        val durationMs: Long,
        val resetSession: Boolean = false,
    ) : EntryImmersiveProgress
}

sealed interface EntryImmersiveHandle {
    val entryType: EntryType
    val chapterId: Long?

    data class ImagePages(
        override val entryType: EntryType,
        override val chapterId: Long?,
        val delegate: Any,
    ) : EntryImmersiveHandle

    data class Playback(
        override val entryType: EntryType,
        override val chapterId: Long?,
        val stream: VideoStream,
        val subtitles: List<VideoSubtitle>,
        val resumePositionMs: Long,
        val delegate: Any? = null,
    ) : EntryImmersiveHandle
}

sealed interface EntryImmersiveSourceAvailability {
    data object Available : EntryImmersiveSourceAvailability

    data object SourceUnavailable : EntryImmersiveSourceAvailability

    data object SourceOptedOut : EntryImmersiveSourceAvailability

    /** No contributed content type currently provides an immersive runtime. */
    data object NoRuntimeType : EntryImmersiveSourceAvailability

    /** Contextual surface pruning from declared metadata only; each returned Entry.type remains authoritative. */
    data class NoCompatibleDeclaredType(val declaredTypes: Set<EntryType>) : EntryImmersiveSourceAvailability
}

sealed interface EntryImmersiveAvailability {
    data class Available(
        val preloadRadius: Int,
        val childRequirement: EntryImmersiveChildRequirement,
    ) : EntryImmersiveAvailability

    data class Inapplicable(val type: EntryType) : EntryImmersiveAvailability

    data class ContextuallyUnavailable(val reason: EntryImmersiveUnavailableReason) : EntryImmersiveAvailability
}

enum class EntryImmersiveChildRequirement {
    NONE,
    FIRST_READING_CHILD,
}

sealed interface EntryImmersiveUnavailableReason {
    data object SourceUnavailable : EntryImmersiveUnavailableReason
    data object SourceOptedOut : EntryImmersiveUnavailableReason
    data object NoReadingChild : EntryImmersiveUnavailableReason
}

sealed interface EntryImmersivePreloadRadiusResult {
    data class Available(val radius: Int) : EntryImmersivePreloadRadiusResult
    data class Inapplicable(val type: EntryType) : EntryImmersivePreloadRadiusResult
}

data class EntryImmersiveLoadRequest(
    val context: android.content.Context,
    val entry: Entry,
    val source: UnifiedSource?,
    val children: List<EntryChapter>,
    val memberIds: List<Long> = emptyList(),
)

sealed interface EntryImmersiveChildRefreshResult {
    data object Refreshed : EntryImmersiveChildRefreshResult

    data class ContextuallyUnavailable(
        val reason: EntryImmersiveUnavailableReason,
    ) : EntryImmersiveChildRefreshResult

    data class Inapplicable(val type: EntryType) : EntryImmersiveChildRefreshResult

    data class Failed(val error: Throwable) : EntryImmersiveChildRefreshResult
}

sealed interface EntryImmersiveLoadResult {
    data class Loaded(
        val handle: EntryImmersiveHandle,
        val child: EntryChapter?,
    ) : EntryImmersiveLoadResult

    data class Inapplicable(val type: EntryType) : EntryImmersiveLoadResult

    data class ContextuallyUnavailable(val reason: EntryImmersiveUnavailableReason) : EntryImmersiveLoadResult

    data class Failed(val error: Throwable) : EntryImmersiveLoadResult
}

sealed interface EntryImmersiveRendererResult {
    data class Available(val renderer: EntryImmersiveRenderer) : EntryImmersiveRendererResult

    data class Failed(val error: Throwable) : EntryImmersiveRendererResult
}

sealed interface EntryImmersiveOpenTargetResult {
    data class Available(val childId: Long) : EntryImmersiveOpenTargetResult
    data object NotOpenable : EntryImmersiveOpenTargetResult
    data class Inapplicable(val type: EntryType) : EntryImmersiveOpenTargetResult
}
