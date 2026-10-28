package eu.kanade.presentation.updates

class UpdatesSelectionState {
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)

    fun reset() {
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun updateRangeSelection(
        selectedIndex: Int,
        isFirstSelection: Boolean,
    ): IntRange {
        return when {
            isFirstSelection -> {
                selectedPositions[0] = selectedIndex
                selectedPositions[1] = selectedIndex
                IntRange.EMPTY
            }

            selectedIndex < selectedPositions[0] -> {
                val range = selectedIndex + 1..<selectedPositions[0]
                selectedPositions[0] = selectedIndex
                range
            }

            selectedIndex > selectedPositions[1] -> {
                val range = (selectedPositions[1] + 1)..<selectedIndex
                selectedPositions[1] = selectedIndex
                range
            }

            else -> IntRange.EMPTY
        }
    }

    fun updateSelectionBounds(
        selectedIndex: Int,
        selected: Boolean,
        firstSelectedIndex: Int,
        lastSelectedIndex: Int,
    ) {
        if (!selected) {
            if (selectedIndex == selectedPositions[0]) {
                selectedPositions[0] = firstSelectedIndex
            } else if (selectedIndex == selectedPositions[1]) {
                selectedPositions[1] = lastSelectedIndex
            }
        } else {
            if (selectedPositions[0] == -1 || selectedIndex < selectedPositions[0]) {
                selectedPositions[0] = selectedIndex
            }
            if (selectedPositions[1] == -1 || selectedIndex > selectedPositions[1]) {
                selectedPositions[1] = selectedIndex
            }
        }
    }
}
