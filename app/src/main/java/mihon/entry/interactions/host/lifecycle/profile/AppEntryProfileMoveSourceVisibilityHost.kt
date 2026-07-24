package mihon.entry.interactions.host.lifecycle.profile

import eu.kanade.domain.source.service.SourcePreferences
import mihon.entry.interactions.EntryProfileMoveSourceVisibilityHost
import mihon.feature.profiles.core.ProfileStore

class AppEntryProfileMoveSourceVisibilityHost(
    private val profileStore: ProfileStore,
) : EntryProfileMoveSourceVisibilityHost {
    override fun makeSourcesVisible(profileId: Long, sourceIds: Set<Long>) {
        if (sourceIds.isEmpty()) return
        val hiddenSources = profileStore.profileStore(profileId)
            .getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, emptySet())
        val updated = hiddenSources.get() - sourceIds.mapTo(mutableSetOf(), Long::toString)
        if (updated != hiddenSources.get()) hiddenSources.set(updated)
    }
}
