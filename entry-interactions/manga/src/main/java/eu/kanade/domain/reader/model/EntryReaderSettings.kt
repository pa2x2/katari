package eu.kanade.domain.reader.model

import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import tachiyomi.domain.entry.model.Entry

val Entry.readingMode: Long
    get() = viewerFlags and ReadingMode.MASK.toLong()

val Entry.readerOrientation: Long
    get() = viewerFlags and ReaderOrientation.MASK.toLong()
