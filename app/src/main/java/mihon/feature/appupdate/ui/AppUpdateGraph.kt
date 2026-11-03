package mihon.feature.appupdate.ui

import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.OnboardingPreferences
import mihon.feature.appupdate.AndroidAppUpdatePlatform
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.mpvaddon.MpvAddonController

/** Graph entry points of the update prompt, which has no view model to inject them. */
interface AppUpdateGraph {
    val appUpdateServices: AppUpdateServices
    val onboardingPreferences: OnboardingPreferences
}

/** Public so the public app graph can return it, while the update feature's Android types stay internal to the app. */
@Inject
class AppUpdateServices internal constructor(
    internal val controller: AppUpdateController,
    internal val platform: AndroidAppUpdatePlatform,
    internal val mpvAddonController: MpvAddonController,
)
