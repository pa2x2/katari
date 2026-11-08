package eu.kanade.presentation.entry.components

import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.CallMerge
import app.katari.icons.materialsymbols.automirroredrounded.CompareArrows
import app.katari.icons.materialsymbols.rounded.Favorite
import app.katari.icons.materialsymbols.rounded.OpenInFull
import app.katari.icons.materialsymbols.roundedfilled.Favorite

object EntryActionIcons {
    val addToLibrary: ImageVector = MaterialSymbols.Rounded.Favorite
    val inLibrary: ImageVector = MaterialSymbols.RoundedFilled.Favorite
    val merge: ImageVector = MaterialSymbols.AutoMirroredRounded.CallMerge
    val migrate: ImageVector = MaterialSymbols.AutoMirroredRounded.CompareArrows
    val openFullEntry: ImageVector = MaterialSymbols.Rounded.OpenInFull

    fun library(favorite: Boolean): ImageVector = if (favorite) inLibrary else addToLibrary
}
