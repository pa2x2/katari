package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal

class GetEnabledCatalogSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
    private val profilePreferences: ProfileSourcePreferences? = null,
) {

    fun subscribe(): Flow<List<Source>> {
        return subscribe(preferences)
    }

    fun subscribe(profileId: Long): Flow<List<Source>> {
        return subscribe(profilePreferences?.forProfile(profileId) ?: preferences)
    }

    private fun subscribe(preferences: SourcePreferences): Flow<List<Source>> {
        return combine(
            preferences.pinnedSources.changes(),
            preferences.enabledLanguages.changes(),
            preferences.disabledSources.changes(),
            preferences.lastUsedSource.changes(),
            repository.getSources(),
        ) { pinnedSourceIds, enabledLanguages, disabledSources, lastUsedSource, sources ->
            sources
                .filter { it.lang in enabledLanguages || it.isLocal() }
                .filterNot { it.id.toString() in disabledSources }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap { source ->
                    val pin = if (source.id.toString() in pinnedSourceIds) {
                        Pins.pinned
                    } else {
                        Pins.unpinned
                    }
                    val isUsedLast = source.id == lastUsedSource
                    val domainSource = source.copy(pin = pin)
                    val toFlatten = mutableListOf(domainSource)
                    if (isUsedLast) {
                        toFlatten.add(domainSource.copy(isUsedLast = true, pin = pin - Pin.Actual))
                    }
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
