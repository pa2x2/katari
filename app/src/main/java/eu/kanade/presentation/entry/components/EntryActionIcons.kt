package eu.kanade.presentation.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.ui.graphics.vector.ImageVector

internal object EntryActionIcons {
    val addToLibrary: ImageVector = Icons.Outlined.FavoriteBorder
    val inLibrary: ImageVector = Icons.Filled.Favorite
    val merge: ImageVector = Icons.AutoMirrored.Outlined.CallMerge
    val migrate: ImageVector = Icons.AutoMirrored.Outlined.CompareArrows
    val openFullEntry: ImageVector = Icons.Outlined.OpenInFull

    fun library(favorite: Boolean): ImageVector = if (favorite) inLibrary else addToLibrary
}
