package mihon.entry.interactions.anime.download.model

import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class AnimeDownload(
    val anime: Entry,
    val episode: EntryChapter,
    val preferences: DownloadPreferences,
) {
    var selection: PlaybackSelection = PlaybackSelection(
        dubKey = preferences.dubKey,
        streamKey = preferences.streamKey,
    )
    var playbackData: PlaybackDescriptor? = null
    var stream: VideoStream? = null
    var subtitles: List<VideoSubtitle> = emptyList()

    @Transient
    private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(status) {
            _statusFlow.value = status
        }

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    @Transient
    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(progress) {
            _progressFlow.value = progress.coerceIn(0, 100)
        }

    @Transient
    private val _failureFlow = MutableStateFlow<AnimeDownloadFailure?>(null)

    @Transient
    val failureFlow = _failureFlow.asStateFlow()
    var failure: AnimeDownloadFailure?
        get() = _failureFlow.value
        set(failure) {
            _failureFlow.value = failure
        }

    enum class State {
        NOT_DOWNLOADED,
        QUEUE,
        RESOLVING,
        DOWNLOADING,
        DOWNLOADED,
        ERROR,
    }
}

internal data class AnimeDownloadFailure(
    val reason: Reason,
    val message: String? = null,
) {
    enum class Reason {
        SOURCE_NOT_FOUND,
        EPISODE_NOT_FOUND,
        PREFERENCES_NOT_SUPPORTED,
        DUB_NOT_AVAILABLE,
        STREAM_NOT_AVAILABLE,
        SUBTITLE_NOT_AVAILABLE,
        QUALITY_NOT_AVAILABLE,
        STREAM_EXPIRED,
        UNSUPPORTED_STREAM,
        INSUFFICIENT_STORAGE,
        NETWORK,
        UNKNOWN,
    }
}
