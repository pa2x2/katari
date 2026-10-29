package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.screen.about.formattedBuildTime
import eu.kanade.presentation.more.settings.screen.about.versionText
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberDateTimeFormatter
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.common.FeatureFlags
import mihon.core.platform.currentPlatformContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Autorenew
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.util.collectAsState

class DebugInfoScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val diagnostics = LocalAppUiGraph.current.deviceDiagnostics
        PreferenceScaffold(
            titleRes = MR.strings.pref_debug_info,
            onBackPressed = navigator::pop,
            itemsProvider = {
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = "Worker info",
                        visible = diagnostics.listsBackgroundWork,
                        onClick = { navigator.push(diagnostics.backgroundWorkScreen()) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = AnimePlayerLauncherScreen.TITLE,
                        onClick = { navigator.push(AnimePlayerLauncherScreen()) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = BackupSchemaScreen.TITLE,
                        onClick = { navigator.push(BackupSchemaScreen()) },
                    ),
                    getAppInfoGroup(),
                    getDeviceInfoGroup(),
                )
            },
        )
    }

    @Composable
    private fun getAppInfoGroup(): Preference.PreferenceGroup {
        val graph = LocalAppUiGraph.current
        val scope = rememberCoroutineScope()

        val installationIdPref = graph.installationPreferences.installationId
        val buildDateTimeFormat = rememberDateTimeFormatter()
        val installationId by installationIdPref.collectAsState()

        return Preference.PreferenceGroup(
            title = "App info",
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = "Version",
                    subtitle = graph.appBuild.versionText(),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Build time",
                    subtitle = graph.appBuild.formattedBuildTime(buildDateTimeFormat),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Installation ID",
                    subtitle = installationId,
                    widget = {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Autorenew,
                            contentDescription = "Regenerate installation ID",
                            onClick = {
                                scope.launch {
                                    installationIdPref.set(FeatureFlags.newInstallationId())
                                }
                            },
                            tone = ContentTone.Primary,
                        )
                    },
                    onClick = {
                        graph.textClipboard.copy(installationId, installationId)
                    },
                ),
                getProfileVerifierPreference(),
                Preference.PreferenceItem.TextPreference(
                    title = "WebView version",
                    subtitle = getWebViewVersion(),
                    visible = graph.webViewData.isOffered,
                ),
            ),
        )
    }

    @Composable
    private fun getWebViewVersion(): String {
        val context = currentPlatformContext()
        val webViewData = LocalAppUiGraph.current.webViewData
        val version by produceState("-") {
            if (webViewData.isOffered) value = webViewData.version(context)
        }
        return version
    }

    @Composable
    private fun getProfileVerifierPreference(): Preference.PreferenceItem.TextPreference {
        val diagnostics = LocalAppUiGraph.current.deviceDiagnostics
        val status by produceState(initialValue = "-") {
            if (diagnostics.reportsProfileCompilation) value = diagnostics.profileCompilationStatus()
        }
        return Preference.PreferenceItem.TextPreference(
            title = "Profile compilation status",
            subtitle = status,
            visible = diagnostics.reportsProfileCompilation,
        )
    }

    @Composable
    private fun getDeviceInfoGroup(): Preference.PreferenceGroup {
        val diagnostics = LocalAppUiGraph.current.deviceDiagnostics
        return Preference.PreferenceGroup(
            title = "Device info",
            preferenceItems = diagnostics.deviceFacts().map { fact ->
                Preference.PreferenceItem.TextPreference(title = fact.title, subtitle = fact.value)
            },
        )
    }
}
