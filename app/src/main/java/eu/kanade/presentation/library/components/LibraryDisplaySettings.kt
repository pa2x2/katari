package eu.kanade.presentation.library.components

import androidx.compose.runtime.Immutable
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle

@Immutable
data class LibraryDisplaySettings(
    val downloadBadge: Boolean = false,
    val unreadBadge: Boolean = true,
    val localBadge: Boolean = true,
    val languageBadge: Boolean = false,
    val entryTypeBadge: Boolean = true,
    val pinnedDisplayStyle: LibraryPinnedDisplayStyle = LibraryPinnedDisplayStyle.TonalGroup,
)
