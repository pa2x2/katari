package mihon.app.ui.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.BasePreferenceStore
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePlatform
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.check.GithubReleaseSource
import tachiyomi.core.common.preference.PreferenceStore

/** The app's own update flow, the same on every platform, which each platform's [AppUpdatePlatform] carries out. */
@ContributesTo(AppScope::class)
@BindingContainer
object AppUpdateControllerBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun appUpdatePreferences(@BasePreferenceStore store: PreferenceStore): AppUpdatePreferences =
        AppUpdatePreferences(store)

    @Provides
    @SingleIn(AppScope::class)
    fun appUpdateController(
        preferences: AppUpdatePreferences,
        network: NetworkHelper,
        json: Json,
        platform: AppUpdatePlatform,
        @AppCoroutineScope scope: CoroutineScope,
    ): AppUpdateController = AppUpdateController(
        preferences = preferences,
        source = GithubReleaseSource(network, json),
        platform = platform,
        scope = scope,
    )
}
