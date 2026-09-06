package eu.kanade.tachiyomi.ui.reader.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Session return point; slider updates within one gesture share the same origin. */
internal class MangaReaderJumpHistory {
    data class Position(val chapterId: Long, val pageIndex: Int)

    private var location: Position? = null
    private var seeking = false
    var returnTarget by mutableStateOf<Position?>(null)
        private set

    fun observe(chapterId: Long, pageIndex: Int) {
        location = Position(chapterId, pageIndex)
    }

    fun rememberOrigin() {
        location?.let { returnTarget = it }
    }

    fun beginSeek(pageIndex: Int) {
        val origin = location ?: return
        if (seeking || origin.pageIndex == pageIndex) return
        rememberOrigin()
        seeking = true
    }

    fun finishSeek() {
        seeking = false
    }

    fun dismiss() {
        returnTarget = null
    }
}
