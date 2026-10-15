package mihon.entry.interactions.runtime.production

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Services the Entry Features read from Injekt until they move into the graph. Production registers them in the app
 * modules and the Entry runtime installation; production validation registers controlled ones.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryHostInjektBindings {
    @Provides
    fun entryInteractions(): EntryInteractions = Injekt.get()

    @Provides
    fun featureRuntimeComposition(): FeatureRuntimeComposition = Injekt.get()

    @Provides
    fun networkHelper(): NetworkHelper = Injekt.get()
}
