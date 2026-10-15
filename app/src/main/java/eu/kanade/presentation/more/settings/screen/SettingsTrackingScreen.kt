package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.tracking.TrackingLoginDialog
import eu.kanade.presentation.more.settings.screen.tracking.TrackingLogoutDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import mihon.entry.interactions.tracking.EntryTrackingAccount
import mihon.entry.interactions.tracking.EntryTrackingAccountOperationResult
import mihon.entry.interactions.tracking.EntryTrackingCredentialIdentity
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingLoginMethod
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.presentation.kit.button.IconButton
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTrackingScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_tracking

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(
            icon = MaterialSymbols.AutoMirroredRounded.Help,
            contentDescription = stringResource(MR.strings.tracking_guide),
            onClick = { uriHandler.openUri("${Constants.URL_GUIDE}tracking/") },
        )
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val trackPreferences = remember { Injekt.get<TrackPreferences>() }
        val trackingFeature = remember { Injekt.get<EntryTrackingFeature>() }
        val scope = rememberCoroutineScope()
        val accountSnapshot by trackingFeature.observeAccounts().collectAsState(trackingFeature.currentAccounts())

        var dialog by remember { mutableStateOf<Any?>(null) }
        var refreshingServices by remember { mutableStateOf(emptySet<EntryTrackingServiceId>()) }
        dialog?.run {
            when (this) {
                is LoginDialog -> {
                    TrackingLoginDialog(
                        account = account,
                        storedUsername = storedUsername,
                        storedPassword = storedPassword,
                        trackingFeature = trackingFeature,
                        onDismissRequest = { dialog = null },
                    )
                }
                is LogoutDialog -> {
                    TrackingLogoutDialog(
                        account = account,
                        trackingFeature = trackingFeature,
                        onDismissRequest = { dialog = null },
                    )
                }
            }
        }

        val accounts = accountSnapshot.accounts
        val regularAccounts = accounts.filter { it.loginMethod !is EntryTrackingLoginMethod.Passive }
        val enhancedAccounts = accounts.filter { it.loginMethod is EntryTrackingLoginMethod.Passive }
        val availableEnhancedAccounts = enhancedAccounts.filter(EntryTrackingAccount::isAvailable)
        var enhancedTrackerInfo = stringResource(MR.strings.enhanced_tracking_info)
        val unavailableEnhancedAccounts = enhancedAccounts.filterNot(EntryTrackingAccount::isAvailable)
        if (unavailableEnhancedAccounts.isNotEmpty()) {
            val missingSourcesInfo = stringResource(
                MR.strings.enhanced_services_not_installed,
                unavailableEnhancedAccounts.joinToString { it.service.name },
            )
            enhancedTrackerInfo += "\n\n$missingSourcesInfo"
        }

        fun beginLogin(account: EntryTrackingAccount) {
            when (account.loginMethod) {
                is EntryTrackingLoginMethod.Credentials -> {
                    val credentials = trackingFeature.storedCredentials(account.service.id) ?: return
                    dialog = LoginDialog(account, credentials.username, credentials.password)
                }
                EntryTrackingLoginMethod.External,
                EntryTrackingLoginMethod.Passive,
                -> scope.launchIO {
                    when (val result = trackingFeature.beginLogin(account.service.id)) {
                        is EntryTrackingAccountOperationResult.ExternalAuthorization -> {
                            withUIContext { context.openInBrowser(result.uri, forceDefaultBrowser = true) }
                        }
                        EntryTrackingAccountOperationResult.Completed -> Unit
                        is EntryTrackingAccountOperationResult.Failed -> {
                            withUIContext { context.toast(result.cause.message.toString()) }
                        }
                        is EntryTrackingAccountOperationResult.Unavailable -> Unit
                    }
                }
            }
        }

        fun refreshAccount(account: EntryTrackingAccount) {
            val serviceId = account.service.id
            if (serviceId in refreshingServices) return
            refreshingServices += serviceId
            scope.launchIO {
                val result = trackingFeature.refreshAccount(serviceId)
                if (result is EntryTrackingAccountOperationResult.Failed) {
                    logcat(LogPriority.ERROR, result.cause) { "Failed to refresh ${account.service.name} profile" }
                }
                withUIContext {
                    refreshingServices -= serviceId
                    when (result) {
                        EntryTrackingAccountOperationResult.Completed -> context.toast(
                            context.stringResource(MR.strings.refresh_tracker_success, account.service.name),
                        )
                        is EntryTrackingAccountOperationResult.Failed -> context.toast(
                            context.stringResource(
                                MR.strings.refresh_tracker_error,
                                account.service.name,
                                result.cause.message.toString(),
                            ),
                        )
                        is EntryTrackingAccountOperationResult.ExternalAuthorization,
                        is EntryTrackingAccountOperationResult.Unavailable,
                        -> Unit
                    }
                }
            }
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = trackPreferences.autoUpdateTrack,
                title = stringResource(MR.strings.pref_auto_update_manga_sync),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = trackPreferences.autoUpdateTrackOnMarkRead,
                entries = AutoTrackState.entries
                    .associateWith { stringResource(it.titleRes) },
                title = stringResource(MR.strings.pref_auto_update_manga_on_mark_read),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.services),
                preferenceItems = regularAccounts.map { account ->
                    Preference.PreferenceItem.TrackingAccountPreference(
                        account = account,
                        isProfileSpecific = true,
                        login = { beginLogin(account) },
                        logout = { dialog = LogoutDialog(account) },
                        refresh = { refreshAccount(account) },
                        isRefreshing = account.service.id in refreshingServices,
                    )
                } + Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.tracking_info)),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.enhanced_services),
                preferenceItems = (
                    availableEnhancedAccounts
                        .map { account ->
                            Preference.PreferenceItem.TrackingAccountPreference(
                                account = account,
                                isProfileSpecific = true,
                                login = { beginLogin(account) },
                                logout = {
                                    scope.launchIO { trackingFeature.logout(account.service.id) }
                                },
                            )
                        } + listOf(Preference.PreferenceItem.InfoPreference(enhancedTrackerInfo))
                    ),
            ),
        )
    }
}

private data class LoginDialog(
    val account: EntryTrackingAccount,
    val storedUsername: String,
    val storedPassword: String,
)

private data class LogoutDialog(
    val account: EntryTrackingAccount,
)
