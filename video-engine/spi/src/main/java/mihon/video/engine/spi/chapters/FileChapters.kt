package mihon.video.engine.spi.chapters

import kotlinx.coroutines.flow.StateFlow

/**
 * A player that reads the chapters stored in the media file, such as an MKV's. They aren't part of Media3's state, so
 * they come as a flow of their own, empty until the file is loaded and for a file without any.
 */
interface FileChapters {
    val fileChapters: StateFlow<List<FileChapter>>
}

/** [title] is null when the file names none. */
data class FileChapter(val startMs: Long, val title: String?)
