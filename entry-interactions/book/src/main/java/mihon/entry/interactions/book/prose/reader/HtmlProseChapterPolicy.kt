package mihon.entry.interactions.book.prose

internal data class ProseChapterSwitchPolicy(
    val completeCurrent: Boolean,
    val resetViewer: Boolean,
)

internal fun proseChapterSwitchPolicy(
    currentIndex: Int,
    destinationIndex: Int,
    explicitSelection: Boolean,
): ProseChapterSwitchPolicy = ProseChapterSwitchPolicy(
    completeCurrent = !explicitSelection && destinationIndex > currentIndex,
    resetViewer = explicitSelection,
)

internal fun shouldStartProseTransitionLoad(
    adjacent: Boolean,
    loadActive: Boolean,
    existingState: HtmlProseChapterLoadState?,
    retry: Boolean,
): Boolean {
    if (!adjacent || loadActive) return false
    return if (retry) {
        existingState is HtmlProseChapterLoadState.Failed
    } else {
        existingState == null
    }
}
