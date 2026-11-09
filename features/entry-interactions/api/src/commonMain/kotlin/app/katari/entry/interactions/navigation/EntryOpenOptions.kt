package app.katari.entry.interactions.navigation

data class EntryOpenOptions(
    val ownerEntryId: Long? = null,
    val bypassMerge: Boolean = false,
    val pageIndex: Int? = null,
    /** Where a video starts, when given; otherwise where watching left off. */
    val startPositionMs: Long? = null,
    val newTask: Boolean = false,
    val clearTop: Boolean = false,
)
