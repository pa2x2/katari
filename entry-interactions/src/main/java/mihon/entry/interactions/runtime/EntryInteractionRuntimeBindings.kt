package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.media.session.EntryMediaSessionIncognitoState
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition

/**
 * Assembles the Entry interactions from the plugins the Entry type modules contribute. Each plugin also declares its
 * type's capabilities to the Feature graph.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionRuntimeBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun interactions(
        plugins: Set<EntryInteractionPlugin>,
    ): EntryInteractions = createEntryInteractions(plugins.toList())

    @Provides
    @ElementsIntoSet
    fun pluginGraphContributors(plugins: Set<EntryInteractionPlugin>): Set<FeatureGraphContributor> = plugins

    @Provides
    @SingleIn(AppScope::class)
    fun composition(
        interactions: EntryInteractions,
        featureRuntime: FeatureRuntimeComposition,
    ): EntryInteractionComposition = EntryInteractionComposition(interactions, featureRuntime)

    @Provides
    @SingleIn(AppScope::class)
    fun imageComponentInstallers(installers: Set<EntryImageComponentInstaller>): EntryImageComponentInstallers =
        EntryImageComponentInstallers(installers.toList())
}
