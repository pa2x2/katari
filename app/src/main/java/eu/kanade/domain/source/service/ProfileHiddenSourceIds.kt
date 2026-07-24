package eu.kanade.domain.source.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.source.service.HiddenSourceIds

class ProfileHiddenSourceIds(
    private val sourcePreferences: SourcePreferences,
    private val profileSourcePreferences: ProfileSourcePreferences,
) : HiddenSourceIds {

    override fun get(): Set<Long> {
        return sourcePreferences.hiddenSourceIds()
    }

    override fun get(profileId: Long): Set<Long> {
        return profileSourcePreferences.forProfile(profileId).hiddenSourceIds()
    }

    override fun subscribe(): Flow<Set<Long>> {
        return sourcePreferences.observeHiddenSourceIds()
    }

    override fun subscribe(profileId: Long): Flow<Set<Long>> {
        return profileSourcePreferences.forProfile(profileId).observeHiddenSourceIds()
    }
}

private fun SourcePreferences.hiddenSourceIds(): Set<Long> {
    return disabledSources.get().mapNotNull(String::toLongOrNull).toSet()
}

private fun SourcePreferences.observeHiddenSourceIds(): Flow<Set<Long>> {
    return disabledSources.changes()
        .map { hiddenSources -> hiddenSources.mapNotNull(String::toLongOrNull).toSet() }
}
