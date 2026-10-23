package mihon.feature.appupdate.ui

import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.mpvaddon.MpvAddonController

/** Graph entry points of the update prompt and About's update rows, which have no view model to inject them. */
interface AppUpdateGraph {
    val appUpdateServices: AppUpdateServices
    val basePreferences: BasePreferences
}

/** Public so the public app graph can return it, while the update feature's types stay internal to the app. */
@Inject
class AppUpdateServices internal constructor(
    internal val controller: AppUpdateController,
    internal val preferences: AppUpdatePreferences,
    internal val mpvAddonController: MpvAddonController,
)
