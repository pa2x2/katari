package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem

/**
 * A key for a lazy layout to follow a slot by while slots are added before it, a string so Android can save it with
 * the layout's position.
 */
internal val ReaderViewerItem.key: String
    get() = when (this) {
        is ReaderViewerItem.Page -> "page:${page.key}:${facing?.key}"
        is ReaderViewerItem.Transition -> "transition:${transition.from.chapter.id}:${transition.direction}"
    }

private val ReaderPage.key: String
    get() = "${chapter.chapter.id}:$index:${if (this is InsertPage) "second" else "first"}"
