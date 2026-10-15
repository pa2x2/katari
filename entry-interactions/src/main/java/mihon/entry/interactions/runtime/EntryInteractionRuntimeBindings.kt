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
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport

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

    @Provides
    @IntoSet
    fun exportInteractions(interactions: () -> EntryInteractions): InjektExport<*> = injektExport(interactions)

    @Provides
    @IntoSet
    fun exportComposition(composition: () -> EntryInteractionComposition): InjektExport<*> =
        injektExport(composition)

    @Provides
    @IntoSet
    fun exportImageComponentInstallers(installers: () -> EntryImageComponentInstallers): InjektExport<*> =
        injektExport(installers)

    @Provides
    @IntoSet
    fun exportActivityTheme(theme: () -> EntryInteractionActivityTheme): InjektExport<*> = injektExport(theme)

    @Provides
    @IntoSet
    fun exportNotificationActions(actions: () -> EntryDownloadNotificationActions): InjektExport<*> =
        injektExport(actions)

    @Provides
    @IntoSet
    fun exportMediaSessionIncognitoState(state: () -> EntryMediaSessionIncognitoState): InjektExport<*> =
        injektExport(state)

    @Provides
    @IntoSet
    fun exportChildGroupFilterDataSource(dataSource: () -> EntryChildGroupFilterDataSource): InjektExport<*> =
        injektExport(dataSource)
}
