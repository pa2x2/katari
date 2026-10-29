package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.ClearDatabaseScreen
import eu.kanade.presentation.more.settings.screen.advanced.UnrestrictedBackgroundWorkRequest
import eu.kanade.presentation.more.settings.screen.debug.DebugInfoScreen
import eu.kanade.presentation.more.settings.screen.debug.rememberLogDump
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.common.GlobalCustomPreferences
import mihon.core.platform.currentPlatformContext
import mihon.entry.interactions.media.EntryViewerSettingsResetResult
import mihon.entry.interactions.merge.EntryMergeConsequenceStatus
import mihon.presentation.kit.feedback.rememberScreenMessages
import okhttp3.Headers
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsAdvancedScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = currentPlatformContext()
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current

        val networkPreferences = graph.networkPreferences
        val libraryPreferences = graph.libraryPreferences
        val globalLibraryPreferences = graph.globalLibraryPreferences
        val globalCustomPreferences = graph.globalCustomPreferences
        val dumpLogs = rememberLogDump()
        val appNotifications = graph.appNotifications

        return listOfNotNull(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_dump_crash_logs),
                subtitle = stringResource(MR.strings.pref_dump_crash_logs_summary),
                onClick = dumpLogs,
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = networkPreferences.verboseLogging,
                title = stringResource(MR.strings.pref_verbose_logging),
                subtitle = stringResource(MR.strings.pref_verbose_logging_summary),
                onValueChanged = {
                    messages.show(MR.strings.requires_app_restart)
                    true
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = networkPreferences.redactSensitiveHeaders,
                title = stringResource(MR.strings.pref_redact_sensitive_headers),
                subtitle = stringResource(MR.strings.pref_redact_sensitive_headers_summary),
                onValueChanged = {
                    messages.show(MR.strings.requires_app_restart)
                    true
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_debug_info),
                onClick = { navigator.push(DebugInfoScreen()) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_onboarding_guide),
                onClick = { navigator.push(OnboardingScreen()) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_manage_notifications),
                visible = appNotifications.isOffered,
                onClick = { appNotifications.openSystemSettings(context) },
            ),
            graph.displayBackendSetting.preferenceItem(),
            getBackgroundActivityGroup(),
            getDataGroup(),
            getNetworkGroup(networkPreferences = networkPreferences),
            getLibraryGroup(
                libraryPreferences = libraryPreferences,
                globalLibraryPreferences = globalLibraryPreferences,
            ),
            getReaderGroup(),
            getExtensionsGroup(globalCustomPreferences = globalCustomPreferences),
        )
    }

    @Composable
    private fun getBackgroundActivityGroup(): Preference.PreferenceGroup {
        val context = currentPlatformContext()
        val messages = rememberScreenMessages()
        val uriHandler = LocalUriHandler.current
        val restrictions = LocalAppUiGraph.current.backgroundWorkRestrictions

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_background_activity),
            visible = restrictions.isOffered,
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_disable_battery_optimization),
                    subtitle = stringResource(MR.strings.pref_disable_battery_optimization_summary),
                    onClick = {
                        when (restrictions.requestUnrestricted(context)) {
                            UnrestrictedBackgroundWorkRequest.Asked -> Unit
                            UnrestrictedBackgroundWorkRequest.SettingMissing ->
                                messages.show(MR.strings.battery_optimization_setting_activity_not_found)
                            UnrestrictedBackgroundWorkRequest.AlreadyUnrestricted ->
                                messages.show(MR.strings.battery_optimization_disabled)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Don't kill my app!",
                    subtitle = stringResource(MR.strings.about_dont_kill_my_app),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val mergeStatusFeature = graph.entryMergeConsequenceStatusFeature
        val mergeStatusFlow = remember(mergeStatusFeature) { mergeStatusFeature.observeStatus() }
        val mergeStatus by mergeStatusFlow.collectAsState(
            initial = EntryMergeConsequenceStatus(0, 0, null),
        )

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_data),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_invalidate_download_cache),
                    subtitle = stringResource(MR.strings.pref_invalidate_download_cache_summary),
                    onClick = {
                        graph.entryDownloadMaintenanceFeature
                            .invalidateCaches()
                        messages.show(MR.strings.download_cache_invalidated)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_database),
                    subtitle = stringResource(MR.strings.pref_clear_database_summary),
                    visible = graph.entryDestructiveRemovalFeature.isOffered,
                    onClick = { navigator.push(ClearDatabaseScreen()) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_retry_merge_maintenance),
                    subtitle = if (mergeStatus.pendingCount == 0L) {
                        stringResource(MR.strings.pref_retry_merge_maintenance_idle)
                    } else {
                        buildString {
                            append(
                                stringResource(
                                    MR.strings.pref_retry_merge_maintenance_summary,
                                    mergeStatus.pendingCount,
                                    mergeStatus.failedCount,
                                ),
                            )
                            mergeStatus.lastFailure?.let { append("\n$it") }
                        }
                    },
                    visible = mergeStatus.pendingCount > 0,
                    onClick = {
                        scope.launch {
                            mergeStatusFeature.retryPending()
                        }
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getNetworkGroup(
        networkPreferences: NetworkPreferences,
    ): Preference.PreferenceGroup {
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val networkHelper = graph.networkHelper
        val webViewData = graph.webViewData

        val userAgentPref = networkPreferences.defaultUserAgent
        val userAgent by userAgentPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieJar.removeAll()
                        messages.show(MR.strings.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_webview_data),
                    visible = webViewData.isOffered,
                    onClick = {
                        try {
                            webViewData.clearCaches(context)
                            scope.launchNonCancellable {
                                webViewData.deleteStorage(context)
                                withUIContext { messages.show(MR.strings.webview_data_deleted) }
                            }
                        } catch (e: Throwable) {
                            logcat(LogPriority.ERROR, e)
                            messages.show(MR.strings.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = networkPreferences.dohProvider,
                    entries = mapOf(
                        -1 to stringResource(MR.strings.disabled),
                        PREF_DOH_CLOUDFLARE to "Cloudflare",
                        PREF_DOH_GOOGLE to "Google",
                        PREF_DOH_ADGUARD to "AdGuard",
                        PREF_DOH_QUAD9 to "Quad9",
                        PREF_DOH_ALIDNS to "AliDNS",
                        PREF_DOH_DNSPOD to "DNSPod",
                        PREF_DOH_360 to "360",
                        PREF_DOH_QUAD101 to "Quad 101",
                        PREF_DOH_MULLVAD to "Mullvad",
                        PREF_DOH_CONTROLD to "Control D",
                        PREF_DOH_NJALLA to "Njalla",
                        PREF_DOH_SHECAN to "Shecan",
                    ),
                    title = stringResource(MR.strings.pref_dns_over_https),
                    onValueChanged = {
                        messages.show(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    preference = userAgentPref,
                    title = stringResource(MR.strings.pref_user_agent_string),
                    onValueChanged = {
                        try {
                            // OkHttp checks for valid values internally
                            Headers.Builder().add("User-Agent", it)
                            messages.show(MR.strings.requires_app_restart)
                        } catch (_: IllegalArgumentException) {
                            messages.show(MR.strings.error_user_agent_string_invalid)
                            return@EditTextPreference false
                        }
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_reset_user_agent_string),
                    visible = remember(userAgent) { userAgent != userAgentPref.defaultValue() },
                    onClick = {
                        userAgentPref.delete()
                        messages.show(MR.strings.requires_app_restart)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryGroup(
        libraryPreferences: LibraryPreferences,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val metadataUpdateRunner = graph.metadataUpdateRunner

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_library),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_refresh_library_covers),
                    onClick = { scope.launch { metadataUpdateRunner.startNow() } },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_reset_viewer_flags),
                    subtitle = stringResource(MR.strings.pref_reset_viewer_flags_summary),
                    onClick = {
                        scope.launchNonCancellable {
                            val success = runCatching {
                                val profileId = graph.activeProfileProvider.activeProfileId
                                check(
                                    graph.entryViewerSettingsFeature.resetProfileOverrides(profileId) ==
                                        EntryViewerSettingsResetResult.Reset,
                                )
                            }.isSuccess
                            withUIContext {
                                val message = if (success) {
                                    MR.strings.pref_reset_viewer_flags_success
                                } else {
                                    MR.strings.pref_reset_viewer_flags_error
                                }
                                messages.show(message)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.updateMangaTitles,
                    title = stringResource(MR.strings.pref_update_library_manga_titles),
                    subtitle = stringResource(MR.strings.pref_update_library_manga_titles_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = globalLibraryPreferences.disallowNonAsciiFilenames,
                    title = stringResource(MR.strings.pref_disallow_non_ascii_filenames),
                    subtitle = stringResource(MR.strings.pref_disallow_non_ascii_filenames_details),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = globalLibraryPreferences.enableChapterNameHash,
                    title = stringResource(MR.strings.pref_download_name_hash),
                    subtitle = stringResource(MR.strings.pref_download_name_hash_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getReaderGroup(): Preference.PreferenceGroup {
        val decodingSettings = LocalAppUiGraph.current.readerDecodingSettings
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reader),
            visible = decodingSettings.isOffered,
            preferenceItems = if (decodingSettings.isOffered) decodingSettings.preferenceItems() else emptyList(),
        )
    }

    @Composable
    private fun getExtensionsGroup(
        globalCustomPreferences: GlobalCustomPreferences,
    ): Preference.PreferenceGroup {
        val graph = LocalAppUiGraph.current
        val trustExtension = graph.trustExtension

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_extensions),
            preferenceItems = listOfNotNull(
                Preference.PreferenceItem.SwitchPreference(
                    preference = globalCustomPreferences.extensionsAutoUpdates,
                    title = stringResource(MR.strings.pref_extensions_auto_update),
                ),
                graph.extensionInstallerSetting.preferenceItem(),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.ext_revoke_trust),
                    onClick = { trustExtension.revokeAll() },
                ),
            ),
        )
    }
}
