package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.SourceItemOrientation

/**
 * Optional capability for sources that provide non-vertical browse thumbnails.
 */
interface SourceItemOrientationProvider {

    val itemOrientation: SourceItemOrientation
        get() = SourceItemOrientation.VERTICAL
}

fun Source.sourceItemOrientation(): SourceItemOrientation {
    return (this as? SourceItemOrientationProvider)?.itemOrientation ?: SourceItemOrientation.VERTICAL
}
