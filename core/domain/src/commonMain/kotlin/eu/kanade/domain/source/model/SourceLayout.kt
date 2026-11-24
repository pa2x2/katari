package eu.kanade.domain.source.model

import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode

/**
 * How a source's catalog, feeds and related entries lay out their covers: the source's own layout while [ownLayout],
 * Browse's otherwise.
 */
data class SourceLayout(
    val displayMode: LibraryDisplayMode,
    val coverSize: CoverSize,
    val ownLayout: Boolean,
)
