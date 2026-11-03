package mihon.entry.interactions.presentation

import eu.kanade.tachiyomi.source.entry.EntryType

internal class DefaultEntryTypePresentationFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryTypePresentationInteraction,
) : EntryTypePresentationFeature {
    override val genericPresentation: EntryTypePresentation = genericEntryTypePresentation

    override fun presentation(type: EntryType?): EntryTypePresentationResult {
        if (type == null || type !in applicableTypes) {
            return EntryTypePresentationResult.Generic(type, genericPresentation)
        }
        return EntryTypePresentationResult.Contributed(
            type = type,
            presentation = checkNotNull(interaction.presentation(type)) {
                "Type-presentation graph selected $type without an operational provider"
            },
        )
    }
}
