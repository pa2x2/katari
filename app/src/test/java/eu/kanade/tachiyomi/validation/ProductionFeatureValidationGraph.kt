package eu.kanade.tachiyomi.validation

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.app.di.ApplicationFeatureGraph
import mihon.tts.provider.android.AndroidTtsBindings
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

/**
 * The production application Features, with the host services the validation environment controls. Android speech
 * engine discovery reads system settings the environment doesn't have; the Feature graph doesn't depend on which
 * engines are installed.
 */
@DependencyGraph(AppScope::class, excludes = [AndroidTtsBindings::class])
interface ProductionFeatureValidationGraph : ApplicationFeatureGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
            @Provides networkHelper: NetworkHelper,
            @Provides profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
        ): ProductionFeatureValidationGraph
    }
}
