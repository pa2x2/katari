package eu.kanade.presentation.entry.components

import androidx.compose.ui.graphics.vector.ImageVector
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.CallMerge
import mihon.icons.materialsymbols.automirroredrounded.CompareArrows
import mihon.icons.materialsymbols.rounded.Favorite
import mihon.icons.materialsymbols.rounded.OpenInFull
import mihon.icons.materialsymbols.roundedfilled.Favorite

internal object EntryActionIcons {
    val addToLibrary: ImageVector = MaterialSymbols.Rounded.Favorite
    val inLibrary: ImageVector = MaterialSymbols.RoundedFilled.Favorite
    val merge: ImageVector = MaterialSymbols.AutoMirroredRounded.CallMerge
    val migrate: ImageVector = MaterialSymbols.AutoMirroredRounded.CompareArrows
    val openFullEntry: ImageVector = MaterialSymbols.Rounded.OpenInFull

    fun library(favorite: Boolean): ImageVector = if (favorite) inLibrary else addToLibrary
}
