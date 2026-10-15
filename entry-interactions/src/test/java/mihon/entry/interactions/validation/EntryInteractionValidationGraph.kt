package mihon.entry.interactions.validation

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.mockk.mockk
import mihon.feature.runtime.FeatureRuntimeGraph
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

/** The Entry Features this module contributes, with the host services the validation environment controls. */
@DependencyGraph(
    AppScope::class,
    bindingContainers = [ControlledEntryHostBindings::class, ControlledInjektBindings::class],
)
interface EntryInteractionValidationGraph : FeatureRuntimeGraph {
    // The translation runtime is an app module, so only the app graph has the real host actions.
    @Provides
    fun translationHostActions(): TranslationHostActions = mockk(relaxed = true)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
            @Provides profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
        ): EntryInteractionValidationGraph
    }
}
