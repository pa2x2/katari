package app.katari.entry.interactions.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.katari.core.platform.PlatformContext
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoStream
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Draws one page's media in the immersive feed, centred in the page, over what the feed shows of the title until the
 * media shows: a renderer paints no background of its own while it loads. [onMediaWidthChange] is told how wide the
 * media is drawn once it shows, or null until then, so the feed can put its title and actions beside media that leaves
 * room; the media's own [switches], such as a clip's subtitles, join the feed's controls. Media that fails offers
 * [onNextTitle] beside its retry, null on the last title.
 */
fun interface EntryImmersiveRenderer {
    @Composable
    fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onPagingBlockedChange: (Boolean) -> Unit,
        place: EntryImmersivePlaceTracker,
        onMediaWidthChange: (Dp?) -> Unit,
        switches: EntryImmersiveMediaSwitches,
        onNextTitle: (() -> Unit)?,
    )
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
        val delegate: Any? = null,
    ) : EntryImmersiveHandle
}

sealed interface EntryImmersiveSourceAvailability {
    data object Available : EntryImmersiveSourceAvailability

    data object SourceUnavailable : EntryImmersiveSourceAvailability

    /** No contributed content type currently provides an immersive runtime. */
    data object NoRuntimeType : EntryImmersiveSourceAvailability

    /** Contextual surface pruning from declared metadata only; each returned Entry.type remains authoritative. */
    data class NoCompatibleDeclaredType(val declaredTypes: Set<EntryType>) : EntryImmersiveSourceAvailability
}

sealed interface EntryImmersiveAvailability {
    data class Available(
        val preloadAhead: Int,
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
    data object NoReadingChild : EntryImmersiveUnavailableReason
}

/** How many titles after the shown one the feed loads and opens, by the shown title's type. */
sealed interface EntryImmersivePreloadAheadResult {
    data class Available(val titles: Int) : EntryImmersivePreloadAheadResult
    data class Inapplicable(val type: EntryType) : EntryImmersivePreloadAheadResult
}

data class EntryImmersiveLoadRequest(
    val context: PlatformContext,
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
