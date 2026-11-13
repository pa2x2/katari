package eu.kanade.tachiyomi.ui.video.player.subtitle.timing

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.subtitlePreferenceKey

/**
 * The key a timing is kept with for the subtitle [selection] shows: a source's file by its label and language, which
 * outlast its URL, and a track in the stream by its own key; null while no subtitle shows.
 */
internal fun subtitleTimingKey(selection: VideoPlayerSubtitleSelection): String? = when (selection) {
    is VideoPlayerSubtitleSelection.External -> subtitlePreferenceKey(selection)
    is VideoPlayerSubtitleSelection.Embedded -> selection.key
    VideoPlayerSubtitleSelection.None, VideoPlayerSubtitleSelection.Default -> null
}
