package eu.kanade.tachiyomi

import dev.zacsweers.metro.Multibinds
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates

/** What the application reads from the graph while it starts. */
interface AppStartupGraph {
    @AppCoroutineScope
    val appScope: CoroutineScope

    val basePreferences: BasePreferences
    val privacyPreferences: PrivacyPreferences
    val networkPreferences: NetworkPreferences
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val preferenceStore: PreferenceStore
    val networkHelper: NetworkHelper
    val extensionManager: ExtensionManager
    val getUpdates: GetUpdates
    val profileDatabase: ProfileDatabase
    val sourceManager: SourceManager
    val database: Database
    val startupWarmups: Set<StartupWarmup>

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
