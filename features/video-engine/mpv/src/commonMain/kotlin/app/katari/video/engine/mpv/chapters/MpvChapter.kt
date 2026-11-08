package app.katari.video.engine.mpv.chapters

import app.katari.video.engine.spi.chapters.FileChapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One entry of mpv's `chapter-list` property. [time] is in seconds. */
@Serializable
internal data class MpvChapter(val time: Double, val title: String? = null) {
    fun toFileChapter() = FileChapter(startMs = (time * 1000).toLong(), title = title?.takeIf(String::isNotBlank))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** mpv writes the property as JSON when it is read as a string. */
        fun parseList(text: String): List<MpvChapter> = json.decodeFromString(text)
    }
}
