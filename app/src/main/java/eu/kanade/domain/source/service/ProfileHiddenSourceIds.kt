package eu.kanade.domain.source.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.source.service.HiddenSourceIds

class ProfileHiddenSourceIds(
    private val sourcePreferences: SourcePreferences,
) : HiddenSourceIds {

    override fun get(): Set<Long> {
        return sourcePreferences.disabledSources.get().mapNotNull(String::toLongOrNull).toSet()
    }

    override fun subscribe(): Flow<Set<Long>> {
        return sourcePreferences.disabledSources.changes()
            .map { hiddenSources -> hiddenSources.mapNotNull(String::toLongOrNull).toSet() }
    }
}
