package eu.kanade.tachiyomi.ui.reader.model

import mihon.entry.interactions.viewer.EntryChildWindow

internal typealias ViewerChapters = EntryChildWindow<ReaderChapter>

internal fun ViewerChapters.ref() {
    current.ref()
    previous?.ref()
    next?.ref()
}

internal fun ViewerChapters.unref() {
    current.unref()
    previous?.unref()
    next?.unref()
}
