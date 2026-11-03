package eu.kanade.tachiyomi.ui.entry.chapters

import eu.kanade.core.util.addOrRemove
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import tachiyomi.domain.entry.model.EntryChapter

/**
 * The chapters selected on an entry screen. Selection is kept by chapter id, so it survives the list being built again,
 * along with the first and last selected rows a long press selects the chapters up to.
 */
class EntryChapterSelection(private val state: EntryStateAccess) {

    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedChapterIds: HashSet<Long> = HashSet()

    fun isSelected(chapterId: Long): Boolean = chapterId in selectedChapterIds

    fun toggle(
        item: EntryChapterList.Item,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        state.update { loaded ->
            val chapters = loaded.processedChapters
            val selectedIndex = loaded.processedChapterIndex(item.id) ?: return@update loaded

            val selectedItem = chapters[selectedIndex]
            if (selectedItem.selected == selected) return@update loaded

            val updates = mutableListOf(selectedItem.copy(selected = selected))
            selectedChapterIds.addOrRemove(item.id, selected)

            if (selected && fromLongPress) {
                if (loaded.selectedChapters.isEmpty()) {
                    selectedPositions[0] = selectedIndex
                    selectedPositions[1] = selectedIndex
                } else {
                    // Try to select the items in-between when possible
                    val range: IntRange
                    if (selectedIndex < selectedPositions[0]) {
                        range = selectedIndex + 1..<selectedPositions[0]
                        selectedPositions[0] = selectedIndex
                    } else if (selectedIndex > selectedPositions[1]) {
                        range = (selectedPositions[1] + 1)..<selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Just select itself
                        range = IntRange.EMPTY
                    }

                    range.forEach { index ->
                        val inbetweenItem = chapters[index]
                        // Chapters in a closed member or group stay out of the range
                        if (!inbetweenItem.selected && loaded.chapterListIndex(inbetweenItem.id) != null) {
                            selectedChapterIds.add(inbetweenItem.id)
                            updates += inbetweenItem.copy(selected = true)
                        }
                    }
                }
            } else if (!fromLongPress) {
                if (!selected) {
                    if (selectedIndex == selectedPositions[0]) {
                        selectedPositions[0] = chapters.indexOfFirst { it.selected && it.id != item.id }
                    } else if (selectedIndex == selectedPositions[1]) {
                        selectedPositions[1] = chapters.indexOfLast { it.selected && it.id != item.id }
                    }
                } else if (selectedIndex < selectedPositions[0]) {
                    selectedPositions[0] = selectedIndex
                } else if (selectedIndex > selectedPositions[1]) {
                    selectedPositions[1] = selectedIndex
                }
            }
            loaded.updateChapterItems(updates)
        }
    }

    fun setAll(selected: Boolean) {
        if (!selected && selectedChapterIds.isEmpty()) return
        state.update { loaded ->
            val updates = loaded.chapters.mapNotNull {
                if (it.selected == selected) return@mapNotNull null
                selectedChapterIds.addOrRemove(it.id, selected)
                it.copy(selected = selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            loaded.updateChapterItems(updates)
        }
    }

    fun invert() {
        state.update { loaded ->
            val updates = loaded.chapters.map {
                selectedChapterIds.addOrRemove(it.id, !it.selected)
                it.copy(selected = !it.selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            loaded.updateChapterItems(updates)
        }
    }

    /** Selects the chapters the filters show that match [predicate]. */
    fun selectShown(predicate: (EntryChapter) -> Boolean) {
        state.update { loaded ->
            val updates = loaded.processedChapters.mapNotNull { item ->
                if (item.selected || !predicate(item.chapter)) return@mapNotNull null
                selectedChapterIds.add(item.id)
                item.copy(selected = true)
            }
            loaded.updateChapterItems(updates).also(::updateSelectedPositions)
        }
    }

    fun deselect(predicate: (EntryChapter) -> Boolean) {
        state.update { loaded ->
            val updates = loaded.chapters.mapNotNull { item ->
                if (!item.selected || !predicate(item.chapter)) return@mapNotNull null
                selectedChapterIds.remove(item.id)
                item.copy(selected = false)
            }
            loaded.updateChapterItems(updates).also(::updateSelectedPositions)
        }
    }

    private fun updateSelectedPositions(loaded: EntryViewModel.State.Success) {
        selectedPositions[0] = loaded.processedChapters.indexOfFirst { it.selected }
        selectedPositions[1] = loaded.processedChapters.indexOfLast { it.selected }
    }
}
