package eu.kanade.tachiyomi.ui.entry

/**
 * The entry screen's state as the models that own one part of the screen read and change it. Until the entry loads
 * there is no state to read, and changes are dropped.
 */
interface EntryStateAccess {
    val loaded: EntryViewModel.State.Success?

    fun update(transform: (EntryViewModel.State.Success) -> EntryViewModel.State.Success)
}
