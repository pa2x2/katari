package app.katari.entry.interactions.host.lifecycle.profile

import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import app.katari.profiles.core.ProfileStore
import eu.kanade.domain.source.service.SourcePreferences

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
