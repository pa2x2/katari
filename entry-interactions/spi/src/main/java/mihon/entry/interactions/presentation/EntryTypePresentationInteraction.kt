package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType

interface EntryTypePresentationInteraction {
    fun presentation(type: EntryType): EntryTypePresentation?
}

internal class ProviderBackedEntryTypePresentationInteraction(
    private val providers: Map<EntryType, EntryTypePresentationProvider>,
) : EntryTypePresentationInteraction {
    override fun presentation(type: EntryType): EntryTypePresentation? = providers[type]?.presentation
}
