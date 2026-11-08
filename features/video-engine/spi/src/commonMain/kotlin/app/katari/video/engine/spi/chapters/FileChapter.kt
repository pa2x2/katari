package app.katari.video.engine.spi.chapters

/** A chapter stored in the media file, such as an MKV's. [title] is null when the file names none. */
data class FileChapter(val startMs: Long, val title: String?)
