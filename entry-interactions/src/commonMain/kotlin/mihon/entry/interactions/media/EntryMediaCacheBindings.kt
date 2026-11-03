package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.runtime.EntryInteractions
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The media cache feature, kept apart from [EntryMediaBindings] so that a platform which doesn't host its entry types'
 * previews still lists, measures and clears the caches they have.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryMediaCacheBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun mediaCacheFeature(
        mediaCacheProviders: Map<EntryType, EntryMediaCacheProvider>,
        interactions: EntryInteractions,
        @BasePreferenceStore preferenceStore: PreferenceStore,
    ): EntryMediaCacheFeature = DefaultEntryMediaCacheFeature(
        applicableTypes = mediaCacheProviders.keys,
        interaction = interactions.mediaCache,
        preferenceStore = preferenceStore,
    )
}
