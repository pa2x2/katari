package tachiyomi.domain.entry.migration

/**
 * What a migration carries of an entry's translation choices: the language the person translates into and whether
 * downloads are translated, never the content language, which describes the source's text, so the target keeps the
 * language its own source declares. A carried choice only ever sets: no language, or downloads left untranslated,
 * leave the target's own.
 */
data class MigrationTranslationChoices(val targetLanguage: String?, val translateDownloads: Boolean) {
    val carriesAnything: Boolean
        get() = targetLanguage != null || translateDownloads

    fun appliedTo(target: MigrationTranslationChoices) = MigrationTranslationChoices(
        targetLanguage = targetLanguage ?: target.targetLanguage,
        translateDownloads = translateDownloads || target.translateDownloads,
    )
}
