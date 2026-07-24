package mihon.entry.interactions

import tachiyomi.domain.entry.model.EntryChapter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

internal fun EntryUpdateNotificationVocabulary.describeLibraryUpdate(
    children: List<EntryChapter>,
): EntryLibraryUpdateNotificationText {
    val displayableNumbers = children.asSequence()
        .filter { child ->
            when (numberPolicy) {
                EntryNotificationChildNumberPolicy.RECOGNIZED_ONLY -> child.isRecognizedNumber
                EntryNotificationChildNumberPolicy.NON_NEGATIVE -> child.chapterNumber >= 0.0
            }
        }
        .sortedBy(EntryChapter::chapterNumber)
        .map { child -> formatChildNumber(child.chapterNumber) }
        .distinct()
        .toList()

    return when (displayableNumbers.size) {
        0 -> EntryLibraryUpdateNotificationText.PluralText(
            resource = childGeneric,
            quantity = children.size,
            arguments = listOf(children.size),
        )
        1 -> describeSingleNumber(displayableNumbers.first(), children.size - displayableNumbers.size)
        else -> describeMultipleNumbers(displayableNumbers)
    }
}

private fun EntryUpdateNotificationVocabulary.describeSingleNumber(
    number: String,
    remaining: Int,
): EntryLibraryUpdateNotificationText {
    return if (remaining == 0) {
        EntryLibraryUpdateNotificationText.StringText(childSingle, listOf(number))
    } else {
        EntryLibraryUpdateNotificationText.StringText(childSingleAndMore, listOf(number, remaining))
    }
}

private fun EntryUpdateNotificationVocabulary.describeMultipleNumbers(
    numbers: List<String>,
): EntryLibraryUpdateNotificationText {
    val displayed = numbers.take(maxDisplayedNumbers)
    val remaining = numbers.size - displayed.size
    return if (remaining > 0) {
        EntryLibraryUpdateNotificationText.PluralText(
            resource = childMultipleAndMore,
            quantity = remaining,
            arguments = listOf(displayed.joinToString(", "), remaining),
        )
    } else {
        EntryLibraryUpdateNotificationText.StringText(
            resource = childMultiple,
            arguments = listOf(displayed.joinToString(", ")),
        )
    }
}

private fun formatChildNumber(number: Double): String {
    return DecimalFormat(
        "#.###",
        DecimalFormatSymbols().apply { decimalSeparator = '.' },
    ).format(number)
}
