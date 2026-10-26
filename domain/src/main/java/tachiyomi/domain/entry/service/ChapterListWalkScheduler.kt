package tachiyomi.domain.entry.service

/** Starts reading the older parts of chapter lists in the background, for every entry whose walk goes on. */
fun interface ChapterListWalkScheduler {
    fun schedule()
}
