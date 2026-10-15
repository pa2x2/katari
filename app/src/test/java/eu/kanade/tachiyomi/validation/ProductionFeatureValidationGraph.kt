package eu.kanade.tachiyomi.validation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.mockk.mockk
import mihon.entry.interactions.validation.ControlledAppServiceBindings
import mihon.entry.interactions.validation.ControlledEntryHostBindings
import mihon.entry.interactions.validation.ControlledEntryPointBindings
import mihon.entry.interactions.validation.ProductionValidationGraph
import mihon.tts.provider.android.AndroidTtsBindings
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.updates.repository.UpdatesRepository

/**
 * The production application Features, with the host services the validation environment controls. Android speech
 * engine discovery reads system settings the environment doesn't have; the Feature graph doesn't depend on which
 * engines are installed.
 */
@DependencyGraph(
    AppScope::class,
    bindingContainers = [
        ControlledEntryHostBindings::class,
        ControlledEntryPointBindings::class,
    ],
    excludes = [AndroidTtsBindings::class],
)
interface ProductionFeatureValidationGraph : ProductionValidationGraph, ViewModelGraph {
    // Only the updates widget reads these, and validation never starts it.
    @Provides
    fun updatesRepository(): UpdatesRepository = mockk(relaxed = true)

    @Provides
    fun hiddenSourceIds(): HiddenSourceIds = mockk(relaxed = true)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes appServices: ControlledAppServiceBindings,
            @Provides profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
        ): ProductionFeatureValidationGraph
    }
}
