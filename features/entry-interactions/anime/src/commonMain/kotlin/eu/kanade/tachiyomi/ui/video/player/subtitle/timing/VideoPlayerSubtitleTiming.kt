package eu.kanade.tachiyomi.ui.video.player.subtitle.timing

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.entry.model.SubtitleTiming
import tachiyomi.domain.entry.repository.SubtitleTimingRepository

/**
 * The subtitle timing of the episode playing, the last of [episodes], for the subtitle it shows, the last of
 * [subtitles] (D004). It is kept for the episode with the subtitle it was set for, so it moves that subtitle's lines
 * only, and syncs and is backed up with the episode. Picking another subtitle resets it; turning subtitles off keeps it
 * for when they are turned back on. An episode that opens with a timing says so ([notice]).
 *
 * Changes show at once and are saved a moment after the last one, as holding a step button changes it many times a
 * second; the save runs on [persistenceScope], so leaving the player right after a change still keeps it.
 */
internal class VideoPlayerSubtitleTiming(
    scope: CoroutineScope,
    private val timings: SubtitleTimingRepository,
    episodes: Flow<Long>,
    subtitles: Flow<VideoPlayerSubtitleSelection>,
    private val persistenceScope: CoroutineScope,
    private val persistenceDispatcher: CoroutineDispatcher,
) {
    private val mutableState = MutableStateFlow(SubtitleTimingState())
    val state: StateFlow<SubtitleTimingState> = mutableState.asStateFlow()

    private val mutableNotice = MutableStateFlow<Long?>(null)

    /** The timing the playing episode opened with, while its notice shows. */
    val notice: StateFlow<Long?> = mutableNotice.asStateFlow()

    // The episode whose saved timing the notice has had its turn for
    private var noticedChapterId: Long? = null
    private var pendingSave: Job? = null

    init {
        scope.launch {
            episodes.distinctUntilChanged().collect { chapterId ->
                mutableNotice.value = null
                mutableState.update { it.copy(chapterId = chapterId, stored = null, loaded = false) }
                val stored = timings.get(chapterId)
                mutableState.update { if (it.chapterId == chapterId) it.copy(stored = stored, loaded = true) else it }
                noticeOpening()
            }
        }
        scope.launch {
            subtitles.map(::subtitleTimingKey).distinctUntilChanged().collect { key ->
                mutableState.update { it.copy(subtitleKey = key) }
                noticeOpening()
            }
        }
    }

    /** Sets the playing subtitle's timing to [delayMs], or none at 0. */
    fun set(delayMs: Long) {
        val current = mutableState.value
        val key = current.subtitleKey ?: return
        if (!current.loaded || current.delayMs == delayMs) return
        val timing = SubtitleTiming(current.chapterId, key, delayMs).takeIf { delayMs != 0L }
        mutableState.value = current.copy(stored = timing)
        changed(current.chapterId)
        save(current.chapterId, timing)
    }

    /**
     * Moves the playing subtitle [steps] steps later, or earlier when negative, from the timing as it is now: a button
     * held repeats faster than the screen shows each step.
     */
    fun step(steps: Int) {
        set(steppedSubtitleTiming(mutableState.value.delayMs + steps * SUBTITLE_TIMING_STEP_MS))
    }

    /** Resets the timing when [selection], which the viewer picked, is another subtitle than the one it was set for. */
    fun subtitlePicked(selection: VideoPlayerSubtitleSelection) {
        val current = mutableState.value
        val stored = current.stored ?: return
        val key = subtitleTimingKey(selection) ?: return
        if (key == stored.subtitleKey) return
        mutableState.value = current.copy(stored = null)
        changed(current.chapterId)
        save(current.chapterId, null)
    }

    fun dismissNotice() {
        mutableNotice.value = null
    }

    // A timing changed while watching is no longer the one the episode opened with
    private fun changed(chapterId: Long) {
        noticedChapterId = chapterId
        mutableNotice.value = null
    }

    // Once per episode, when the timing it was saved with applies to the subtitle it opened on
    private fun noticeOpening() {
        val current = mutableState.value
        if (!current.loaded || noticedChapterId == current.chapterId || current.delayMs == 0L) return
        noticedChapterId = current.chapterId
        mutableNotice.value = current.delayMs
    }

    private fun save(chapterId: Long, timing: SubtitleTiming?) {
        val previous = pendingSave
        pendingSave = persistenceScope.launch(persistenceDispatcher) {
            // Cancels a save still waiting; one already writing finishes first, so the writes keep their order
            previous?.cancelAndJoin()
            delay(SAVE_DELAY_MS)
            withContext(NonCancellable) {
                if (timing == null) timings.delete(chapterId) else timings.set(timing)
            }
        }
    }

    private companion object {
        const val SAVE_DELAY_MS = 600L
    }
}

/**
 * The timing of the episode [chapterId] as the player applies it: [stored] once [loaded], moving the subtitle playing,
 * keyed [subtitleKey], when it is the one the timing was set for.
 */
internal data class SubtitleTimingState(
    val chapterId: Long = -1L,
    val subtitleKey: String? = null,
    val stored: SubtitleTiming? = null,
    val loaded: Boolean = false,
) {
    /** How much later the playing subtitle shows, in milliseconds; negative for earlier. */
    val delayMs: Long
        get() = stored?.takeIf { subtitleKey != null && it.subtitleKey == subtitleKey }?.delayMs ?: 0L

    /** A subtitle is on, whose timing can be set. */
    val adjustable: Boolean
        get() = loaded && subtitleKey != null

    /** [delayMs] for the player of [chapterId]: none while the timing of the episode before it is still held. */
    fun delayFor(chapterId: Long): Long = if (chapterId == this.chapterId) delayMs else 0L
}
