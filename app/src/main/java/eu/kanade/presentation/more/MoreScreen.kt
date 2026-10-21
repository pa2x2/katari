package eu.kanade.presentation.more

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import mihon.feature.profiles.ui.switcher.ActiveProfileCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.rounded.Sync
import mihon.presentation.kit.content.NotificationBadge
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickTracking: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    hasNewRecap: Boolean,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(contentPadding = contentPadding) {
            item {
                LogoHeader(
                    iconPadding = PaddingValues(vertical = KatariTheme.spacing.xxl),
                    below = {
                        ActiveProfileCard(
                            modifier = Modifier.padding(
                                start = KatariTheme.spacing.m,
                                end = KatariTheme.spacing.m,
                                bottom = KatariTheme.spacing.m,
                            ),
                        )
                    },
                )
            }

            item {
                RowGroupItem(RowGroupPosition.First) {
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(MR.strings.downloaded_only_summary),
                        icon = MaterialSymbols.Rounded.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChanged = onDownloadedOnlyChange,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Last) {
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                        icon = MaterialSymbols.Rounded.Eyeglasses2,
                        checked = incognitoMode,
                        onCheckedChanged = onIncognitoModeChange,
                    )
                }
            }
            item { GroupGap() }

            item {
                RowGroupItem(RowGroupPosition.First) {
                    val downloadQueueState = downloadQueueStateProvider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null
                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }
                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                            }
                        },
                        icon = MaterialSymbols.Rounded.Download,
                        onPreferenceClick = onClickDownloadQueue,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Middle) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.pref_category_tracking),
                        icon = MaterialSymbols.Rounded.Sync,
                        onPreferenceClick = onClickTracking,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Middle) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.categories),
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        onPreferenceClick = onClickCategories,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Middle) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_stats),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        widget = if (hasNewRecap) {
                            {
                                val description = stringResource(MR.strings.statistics_recap_new)
                                NotificationBadge(
                                    modifier = Modifier.semantics { contentDescription = description },
                                )
                            }
                        } else {
                            null
                        },
                        onPreferenceClick = onClickStats,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Last) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = MaterialSymbols.Rounded.Storage,
                        onPreferenceClick = onClickDataAndStorage,
                    )
                }
            }
            item { GroupGap() }

            item {
                RowGroupItem(RowGroupPosition.First) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_settings),
                        icon = MaterialSymbols.Rounded.Settings,
                        onPreferenceClick = onClickSettings,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Middle) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = MaterialSymbols.Rounded.Info,
                        onPreferenceClick = onClickAbout,
                    )
                }
            }
            item {
                RowGroupItem(RowGroupPosition.Last) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_help),
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupGap() {
    Spacer(Modifier.height(KatariTheme.spacing.m))
}
