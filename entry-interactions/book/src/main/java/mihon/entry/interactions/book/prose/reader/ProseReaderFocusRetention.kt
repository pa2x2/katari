package mihon.entry.interactions.book.prose

internal fun shouldRetainProseTextFocus(
    focusedSectionKey: String?,
    retainedSectionKeys: Set<String>,
    resetViewer: Boolean,
): Boolean {
    return !resetViewer &&
        focusedSectionKey != null &&
        focusedSectionKey in retainedSectionKeys
}
