package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class BookReaderLayoutMode(
    val serializedValue: String,
    val labelRes: StringResource,
) {
    PAGINATED(
        serializedValue = "paginated",
        labelRes = MR.strings.book_reader_layout_paginated,
    ),
    SCROLLING(
        serializedValue = "scrolling",
        labelRes = MR.strings.book_reader_layout_scrolling,
    ),
    ;

    companion object {
        val supportedValues: Set<String> = entries.mapTo(mutableSetOf(), BookReaderLayoutMode::serializedValue)

        fun fromSerializedValue(value: String?): BookReaderLayoutMode =
            entries.find { it.serializedValue == value } ?: PAGINATED
    }
}
