package mihon.app.ui.host

import app.katari.sync.client.setting.capture.SettingsCapture
import dev.zacsweers.metro.Multibinds
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.app.ui.buildinfo.AppBuild
import mihon.core.metro.StartupWarmup
import mihon.core.migration.MigrationGraph
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager

/** What [AppProcessStart] reads from the graph of the platform it starts. */
interface AppProcessGraph : MigrationGraph {
    val appBuild: AppBuild
    val networkPreferences: NetworkPreferences

    /** The store the version the app-upgrade migrations last ran for is kept in. */
    val preferenceStore: PreferenceStore
    val logDestinations: LogDestinations
    val networkHelper: NetworkHelper
    val sourceManager: SourceManager
    val database: Database
    val startupWarmups: Set<StartupWarmup>
    val settingsCapture: SettingsCapture

    /**
     * Profile copy and backup derive preference ownership from every registered owner, so owners register at startup
     * instead of when their module is first used. Building the set is what registers them.
     */
    val profilePreferenceOwners: Set<InstalledProfilePreferenceOwner<*>>

    @Multibinds(allowEmpty = true)
    fun startupWarmupContributions(): Set<StartupWarmup>

    @Multibinds(allowEmpty = true)
    fun profilePreferenceOwnerContributions(): Set<InstalledProfilePreferenceOwner<*>>
}
