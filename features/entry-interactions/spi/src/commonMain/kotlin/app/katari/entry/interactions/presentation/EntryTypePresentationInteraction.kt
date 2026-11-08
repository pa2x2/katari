package app.katari.entry.interactions.presentation

import app.katari.entry.interactions.runtime.EntryTypePresentationProvider
import eu.kanade.tachiyomi.source.entry.EntryType

interface EntryTypePresentationInteraction {
    fun presentation(type: EntryType): EntryTypePresentation?
}

internal class ProviderBackedEntryTypePresentationInteraction(
    private val providers: Map<EntryType, EntryTypePresentationProvider>,
) : EntryTypePresentationInteraction {
    override fun presentation(type: EntryType): EntryTypePresentation? = providers[type]?.presentation
}
