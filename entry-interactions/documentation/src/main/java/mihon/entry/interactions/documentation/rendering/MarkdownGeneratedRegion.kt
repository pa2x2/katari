package mihon.entry.interactions.documentation.rendering

internal fun replaceGeneratedMarkdownRegion(
    document: String,
    generatedMarkdown: String,
    startMarker: String,
    endMarker: String,
): String {
    val startIndex = document.singleMarkerIndex(startMarker)
    val endIndex = document.singleMarkerIndex(endMarker)
    require(startIndex < endIndex) { "$startMarker must appear before $endMarker" }

    val before = document.substring(0, startIndex + startMarker.length)
    val after = document.substring(endIndex)
    return "$before\n\n${generatedMarkdown.trim()}\n\n$after"
}

internal fun removeGeneratedMarkdownRegion(
    document: String,
    startMarker: String,
    endMarker: String,
): String {
    val startIndex = document.singleMarkerIndex(startMarker)
    val endIndex = document.singleMarkerIndex(endMarker)
    require(startIndex < endIndex) { "$startMarker must appear before $endMarker" }
    return document.removeRange(startIndex, endIndex + endMarker.length)
}

private fun String.singleMarkerIndex(marker: String): Int {
    val first = indexOf(marker)
    require(first >= 0) { "Missing generated-region marker: $marker" }
    require(indexOf(marker, first + marker.length) < 0) { "Duplicate generated-region marker: $marker" }
    return first
}
