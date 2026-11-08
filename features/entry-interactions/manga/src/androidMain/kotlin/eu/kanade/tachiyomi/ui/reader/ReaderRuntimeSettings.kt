package eu.kanade.tachiyomi.ui.reader

internal data class ReaderRuntimeSettings(
    val entryId: Long,
    val readingMode: Int,
    val orientation: Int,
) {
    fun changeFrom(previous: ReaderRuntimeSettings?): Change {
        return when {
            previous == null ||
                entryId != previous.entryId ||
                readingMode != previous.readingMode -> Change.RecreateViewer
            orientation != previous.orientation -> Change.UpdateOrientation
            else -> Change.None
        }
    }

    enum class Change {
        RecreateViewer,
        UpdateOrientation,
        None,
    }
}
