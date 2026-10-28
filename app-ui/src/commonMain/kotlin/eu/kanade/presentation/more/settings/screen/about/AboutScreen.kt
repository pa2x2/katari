package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberDateTimeFormatter
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.icons.simpleicons.Github
import mihon.icons.simpleicons.SimpleIcons
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.i18n.stringResource

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val appBuild = graph.appBuild
        val deviceDiagnostics = graph.deviceDiagnostics
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val buildDateTimeFormat = rememberDateTimeFormatter()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val versionRows = buildList<@Composable () -> Unit> {
                add {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        subtitle = appBuild.versionText(buildDateTimeFormat),
                        onPreferenceClick = if (deviceDiagnostics.isOffered) {
                            {
                                scope.launch {
                                    val deviceInfo = deviceDiagnostics.debugInfo(context)
                                    graph.textClipboard.copy("Debug information", deviceInfo)
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                addAll(graph.appUpdateSettings.aboutRows())
            }
            val linkRows = buildList<@Composable () -> Unit> {
                appBuild.releaseNotesUrl?.let { releaseNotesUrl ->
                    add {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.whats_new),
                            onPreferenceClick = { uriHandler.openUri(releaseNotesUrl) },
                        )
                    }
                }
                add {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }
            }
            RowList(
                contentPadding = contentPadding,
            ) {
                item {
                    LogoHeader(
                        iconPadding = PaddingValues(vertical = KatariTheme.spacing.xxl + KatariTheme.spacing.xl),
                    )
                }

                rowGroupItems(versionRows) { it() }
                item { RowGroupGap() }
                rowGroupItems(linkRows) { it() }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = KatariTheme.spacing.s),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LinkIcon(
                            label = "Katari",
                            icon = SimpleIcons.Github,
                            url = Constants.URL_REPOSITORY,
                        )
                    }
                }
            }
        }
    }
}
