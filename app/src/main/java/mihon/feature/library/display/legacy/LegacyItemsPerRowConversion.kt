package mihon.feature.library.display.legacy

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * Turns the old items per row into the cover size that keeps that many columns on a phone in portrait.
 *
 * Folded and unfolded screens are both portrait, so the portrait count is the one people chose for everyday use; the
 * landscape count is dropped. A store with the portrait count replaces its cover size, so a restored backup's choice
 * wins.
 */
object LegacyItemsPerRowConversion {

    fun convert(store: PreferenceStore) {
        val portrait = store.getInt(LibraryPreferences.LEGACY_PORTRAIT_COLUMNS_PREF_KEY, 0)
        val landscape = store.getInt(LibraryPreferences.LEGACY_LANDSCAPE_COLUMNS_PREF_KEY, 0)
        if (portrait.isSet()) {
            LibraryPreferences(store).coverSize.set(coverSizeFor(portrait.get()))
        }
        portrait.delete()
        landscape.delete()
    }

    private fun coverSizeFor(itemsPerRow: Int): CoverSize = when {
        itemsPerRow <= 0 -> CoverSize.Auto
        itemsPerRow == 1 -> CoverSize.ExtraLarge
        itemsPerRow == 2 -> CoverSize.Large
        itemsPerRow == 3 -> CoverSize.Medium
        else -> CoverSize.Small
    }
}
