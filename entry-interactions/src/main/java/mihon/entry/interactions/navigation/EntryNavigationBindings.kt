package mihon.entry.interactions.navigation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

@ContributesTo(AppScope::class)
@BindingContainer
object EntryNavigationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun openFeature(composition: FeatureRuntimeComposition, interactions: EntryInteractions): EntryOpenFeature =
        DefaultEntryOpenFeature(
            evaluation = composition.evaluation,
            interaction = interactions.open,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun continueFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        childOwnership: EntryChildOwnershipResolutionPort,
        chapterRepository: EntryChapterRepository,
        progressRepository: EntryProgressRepository,
    ): EntryContinueFeature = DefaultEntryContinueFeature(
        evaluation = composition.evaluation,
        interaction = interactions.continueEntry,
        batchPreparation = DefaultEntryContinueBatchPreparation(
            childOwnership = childOwnership,
            chapterRepository = chapterRepository,
            progressRepository = progressRepository,
        ),
    )

    @Provides
    @IntoSet
    fun openGraphContributor(): FeatureGraphContributor = EntryOpenFeatureContributor

    @Provides
    @IntoSet
    fun continueGraphContributor(): FeatureGraphContributor = EntryContinueFeatureContributor

    @Provides
    @IntoSet
    fun exportOpenFeature(feature: () -> EntryOpenFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportContinueFeature(feature: () -> EntryContinueFeature): InjektExport<*> = injektExport(feature)
}
