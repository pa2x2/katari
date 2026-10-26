package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberDateTimeFormatter
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.appGraph
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.ui.appUpdateAboutRows
import mihon.icons.simpleicons.Github
import mihon.icons.simpleicons.SimpleIcons
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
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
                        subtitle = getVersionName(buildDateTimeFormat),
                        onPreferenceClick = {
                            scope.launch {
                                // Loads the WebView package's label through the package manager
                                val deviceInfo =
                                    withContext(Dispatchers.IO) {
                                        context.appGraph.crashLogUtilFactory.create(context).getDebugInfo()
                                    }
                                context.copyToClipboard("Debug information", deviceInfo)
                            }
                        },
                    )
                }
                addAll(appUpdateAboutRows())
            }
            val linkRows = buildList<@Composable () -> Unit> {
                if (!BuildConfig.DEBUG) {
                    add {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.whats_new),
                            onPreferenceClick = { uriHandler.openUri(AppUpdateBuild.releasePageUrl) },
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
            ScrollbarLazyColumn(
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

    /** @param buildDateTimeFormat appends the build time in this format, or null to leave it out. */
    fun getVersionName(buildDateTimeFormat: DateTimeFormatter? = null): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (buildDateTimeFormat != null) {
                        "$it (${getFormattedBuildTime(buildDateTimeFormat)})"
                    } else {
                        it
                    }
                }
            }
            isPreviewBuildType -> {
                "Beta r${BuildConfig.COMMIT_COUNT}".let {
                    if (buildDateTimeFormat != null) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime(buildDateTimeFormat)})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            else -> {
                "Stable ${BuildConfig.VERSION_NAME}".let {
                    if (buildDateTimeFormat != null) {
                        "$it (${getFormattedBuildTime(buildDateTimeFormat)})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(dateTimeFormat: DateTimeFormatter): String {
        return try {
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(dateTimeFormat)
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
