package mihon.app.di.injekt

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Host services that are still registered in Injekt, read by graph bindings until they move into the graph. */
@BindingContainer
object InjektHostBindings {
    @Provides
    fun networkHelper(): NetworkHelper = Injekt.get()

    @Provides
    @SingleIn(AppScope::class)
    fun profilePreferenceOwners(): ProfilePreferenceOwnerInstaller =
        ProfilePreferenceOwnerInstaller(Injekt.get()) { Injekt.get<ProfileStore>().profileStore() }
}
