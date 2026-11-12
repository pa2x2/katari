package app.katari.ui.host

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.extension.InstalledExtensionLog
import app.katari.sync.client.restore.RestoreStamping
import app.katari.sync.client.trigger.SyncTriggers
import app.katari.sync.ui.notice.SyncNotices
import app.katari.ui.buildinfo.AppBuild
import app.katari.ui.telemetry.AppStateReader
import app.katari.upgrade.MigrationGraph
import dev.zacsweers.metro.Multibinds
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager

/** What [AppProcessStart] reads from the graph of the platform it starts. */
interface AppProcessGraph : MigrationGraph {
    @AppCoroutineScope
    val appScope: CoroutineScope

    val appBuild: AppBuild
    val networkPreferences: NetworkPreferences
    val privacyPreferences: PrivacyPreferences

    /** The store of the app's own state, such as the version the app-upgrade migrations last ran for. */
    val preferenceStore: PreferenceStore
    val logDestinations: LogDestinations
    val networkHelper: NetworkHelper
    val sourceManager: SourceManager
    val database: Database
    val startupWarmups: Set<StartupWarmup>
    val syncCapture: SyncCapture
    val restoreStamping: RestoreStamping
    val syncTriggers: SyncTriggers
    val syncNotices: SyncNotices
    val installedExtensionLog: InstalledExtensionLog
    val appStateReader: AppStateReader

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
