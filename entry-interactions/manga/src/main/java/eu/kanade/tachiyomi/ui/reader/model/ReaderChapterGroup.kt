package eu.kanade.tachiyomi.ui.reader.model

/** The group of its entry a reader chapter belongs to, such as a volume, so a transition into another can name it. */
internal data class ReaderChapterGroup(
    val key: String,
    val title: String,
)
