package mihon.feature.library.update.planning

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.feature.library.update.pause.LibrarySourcePauses
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/** Everything a profile has set about what library updates check, read once per update. */
data class LibraryUpdateSettings(
    val skipRules: LibraryUpdateSkipRules,
    /** Hours between automatic updates; 0 when the library is not updated automatically. */
    val intervalHours: Int,
    /** Sources paused at the time the settings were read. */
    val pausedSourceIds: Set<Long>,
    val excludedEntryTypes: Set<EntryType>,
    /** Categories whose rules differ from the default. */
    val categoryRules: Map<Long, CategoryUpdateRules>,
    /** Entries whose mode is not [EntryUpdateMode.FOLLOW_RULES]. */
    val entryModes: Map<Long, EntryUpdateMode>,
) {
    /**
     * How often the automatic update has to run so every category is checked at its own interval, or null when nothing
     * is updated automatically.
     */
    val schedulePeriodHours: Int?
        get() = schedulePeriodHours(intervalHours, categoryRules.values)

    companion object {
        fun schedulePeriodHours(intervalHours: Int, categoryRules: Collection<CategoryUpdateRules>): Int? {
            return (categoryRules.filter { it.autoUpdate }.mapNotNull { it.overrides?.intervalHours } + intervalHours)
                .filter { it > 0 }
                .minOrNull()
        }
    }

    class Reader(
        private val libraryPreferences: LibraryPreferences,
        private val rulesRepository: LibraryUpdateRulesRepository,
    ) {
        private val sourcePauses = LibrarySourcePauses(libraryPreferences)

        /** @param now epoch milliseconds the update starts at, which decides whether a source's pause has ended. */
        suspend fun read(now: Long): LibraryUpdateSettings = LibraryUpdateSettings(
            skipRules = LibraryPreferences.skipRulesOf(libraryPreferences.updateSkipRules.get()),
            intervalHours = libraryPreferences.autoUpdateInterval.get(),
            pausedSourceIds = sourcePauses.active(now).keys,
            excludedEntryTypes = libraryPreferences.updateExcludedEntryTypes.get(),
            categoryRules = rulesRepository.getCategoryRules(),
            entryModes = rulesRepository.getEntryModes(),
        )

        /**
         * Emits when a library-wide setting or a category's rules change, for screens that show what an update
         * would do. Entry modes are left out: they change on other screens, and these screens read them again when
         * they are opened.
         */
        fun changes(): Flow<Unit> = combine(
            libraryPreferences.updateSkipRules.changes(),
            libraryPreferences.autoUpdateInterval.changes(),
            libraryPreferences.updatePausedSources.changes(),
            libraryPreferences.updateExcludedEntryTypes.changes(),
            rulesRepository.subscribeCategoryRules(),
        ) { _, _, _, _, _ -> }
    }
}
