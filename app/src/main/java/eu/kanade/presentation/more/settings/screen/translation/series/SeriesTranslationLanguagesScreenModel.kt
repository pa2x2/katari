package eu.kanade.presentation.more.settings.screen.translation.series

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The series of the active profile that use their own translation languages, and clearing them. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SeriesTranslationLanguagesScreenModel(
    private val repository: EntryTranslationLanguagesRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val activeProfile: ActiveProfileProvider = Injekt.get(),
) : ScreenModel {
    /** Ordered by series title; null until first loaded. */
    val series: StateFlow<List<SeriesTranslationLanguages>?> = activeProfile.activeProfileIdFlow
        .flatMapLatest(::seriesOf)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Makes the series follow the default languages again. */
    fun clear(entryId: Long) {
        screenModelScope.launch { repository.clearLanguages(entryId, System.currentTimeMillis()) }
    }

    fun clearAll() {
        screenModelScope.launch {
            repository.clearLanguagesByProfile(activeProfile.activeProfileId, System.currentTimeMillis())
        }
    }

    private fun seriesOf(profileId: Long): Flow<List<SeriesTranslationLanguages>> =
        repository.subscribeByProfile(profileId).mapLatest { records ->
            val entries = entryRepository.getEntriesByIds(records.map { it.entryId }).associateBy { it.id }
            records.mapNotNull { record ->
                val entry = entries[record.entryId] ?: return@mapNotNull null
                SeriesTranslationLanguages(
                    entryId = entry.id,
                    title = entry.title,
                    cover = entry.asEntryCover(),
                    contentLanguage = record.contentLanguage?.let(LanguageTag::parse),
                    targetLanguage = record.targetLanguage?.let(LanguageTag::parse),
                )
            }
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal data class SeriesTranslationLanguages(
    val entryId: Long,
    val title: String,
    val cover: EntryCover,
    /** The language the series' text is kept as; null follows its source. */
    val contentLanguage: LanguageTag?,
    /** The language the series is translated into; null follows the profile. */
    val targetLanguage: LanguageTag?,
)
