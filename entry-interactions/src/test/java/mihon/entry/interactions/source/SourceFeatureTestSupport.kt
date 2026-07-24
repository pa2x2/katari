package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.SpecializedAdapter

internal fun sourceFeatureEvaluation(
    vararg contributors: FeatureGraphContributor,
    specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
): FeatureGraphEvaluation {
    val plugin = object : EntryInteractionPlugin {
        override val type = EntryType.BOOK
        override val owner = ContributionOwner("test.source-feature")
        override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        override val specializedAdapters = specializedAdapters
    }
    return createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = contributors.toList(),
    ).featureGraphEvaluation
}
