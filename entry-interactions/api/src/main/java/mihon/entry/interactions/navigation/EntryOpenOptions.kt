package mihon.entry.interactions

data class EntryOpenOptions(
    val ownerEntryId: Long? = null,
    val bypassMerge: Boolean = false,
    val pageIndex: Int? = null,
    val newTask: Boolean = false,
    val clearTop: Boolean = false,
)
