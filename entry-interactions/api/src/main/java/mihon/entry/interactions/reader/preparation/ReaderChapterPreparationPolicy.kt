package mihon.entry.interactions.reader.preparation

object ReaderChapterPreparationPolicy {
    const val THRESHOLD = 0.75

    fun shouldPrepare(
        enabled: Boolean,
        progression: Double,
    ): Boolean = enabled && progression >= THRESHOLD
}
