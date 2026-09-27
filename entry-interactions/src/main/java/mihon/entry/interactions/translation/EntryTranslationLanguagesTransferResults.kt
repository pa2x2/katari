package mihon.entry.interactions.translation

import eu.kanade.tachiyomi.source.entry.EntryType

internal sealed interface EntryTranslationLanguagesSnapshotResult {
    data class Captured(val snapshot: EntryTranslationLanguagesSnapshot) : EntryTranslationLanguagesSnapshotResult

    data object NoLanguages : EntryTranslationLanguagesSnapshotResult

    data class Inapplicable(val type: EntryType) : EntryTranslationLanguagesSnapshotResult
}

internal sealed interface EntryTranslationLanguagesMigrationPreparation {
    data class Prepared(
        val payload: EntryTranslationLanguagesMigrationPayload,
    ) : EntryTranslationLanguagesMigrationPreparation

    data object NoTargetLanguage : EntryTranslationLanguagesMigrationPreparation

    data class Inapplicable(val types: Set<EntryType>) : EntryTranslationLanguagesMigrationPreparation

    data class TypeMismatch(
        val sourceType: EntryType,
        val targetType: EntryType,
    ) : EntryTranslationLanguagesMigrationPreparation
}
