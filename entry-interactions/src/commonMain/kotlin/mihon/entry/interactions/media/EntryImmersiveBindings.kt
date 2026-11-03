package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.source.EntrySourceRefreshFeature

/**
 * Immersive browsing, kept apart from the other media features because it carries nothing through migrations or
 * backups, so a platform without them still gets it.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryImmersiveBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun immersiveFeature(
        immersiveProcessors: Map<EntryType, EntryImmersiveProcessor>,
        childListProcessors: Map<EntryType, EntryChildListProcessor>,
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        interactions: EntryInteractions,
        childList: EntryChildListFeature,
        sourceRefresh: EntrySourceRefreshFeature,
    ): EntryImmersiveFeature = DefaultEntryImmersiveFeature(
        types = EntryImmersiveTypes(
            immersive = immersiveProcessors.keys,
            childList = immersiveProcessors.keys intersect childListProcessors.keys,
            open = immersiveProcessors.keys intersect openProcessors.keys,
        ),
        interaction = interactions.immersive,
        childList = childList,
        sourceRefresh = sourceRefresh,
    )
}
