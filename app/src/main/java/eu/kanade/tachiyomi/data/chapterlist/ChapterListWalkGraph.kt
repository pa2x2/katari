package eu.kanade.tachiyomi.data.chapterlist

/** Graph entry point of the chapter list walk job, which WorkManager constructs. */
interface ChapterListWalkGraph {
    fun inject(job: ChapterListWalkJob)
}
