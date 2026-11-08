package app.katari.entry.interactions.media

import app.katari.entry.interactions.child.EntryChildListFeature
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.runtime.EntryChildListProcessor
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

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
