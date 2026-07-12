package eu.kanade.presentation.library.components

import androidx.compose.runtime.Immutable

@Immutable
data class LibraryDisplaySettings(
    val downloadBadge: Boolean = false,
    val unreadBadge: Boolean = true,
    val localBadge: Boolean = true,
    val languageBadge: Boolean = false,
    val entryTypeBadge: Boolean = true,
)
