package mihon.entry.interactions.persistence.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition

@ContributesTo(AppScope::class)
@BindingContainer
object EntryBackupBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(composition: FeatureRuntimeComposition): EntryBackupFeature =
        EntryBackupCoordinator(composition.executions)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryBackupFeatureContributor
}
