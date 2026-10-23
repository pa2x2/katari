package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.presentation.browse.components.ContentWarningDialog
import eu.kanade.presentation.browse.components.ExtensionDetailsHeader
import eu.kanade.presentation.browse.components.ExtensionSourceConflictRow
import eu.kanade.presentation.browse.components.ExtensionSourceRow
import eu.kanade.presentation.browse.components.label
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsSourceUiModel
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsState
import kotlinx.collections.immutable.ImmutableList
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Cookie
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.WarningCard
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsState.Success,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickUseForSource: (sourceKey: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_extension_info),
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        actions = listOf(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_enable_all),
                                onClick = onClickEnableAll,
                                icon = MaterialSymbols.Rounded.DoneAll,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_disable_all),
                                onClick = onClickDisableAll,
                                icon = MaterialSymbols.Rounded.RemoveDone,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.pref_clear_cookies),
                                onClick = onClickClearCookies,
                                icon = MaterialSymbols.Rounded.Cookie,
                                group = 1,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        ExtensionDetails(
            contentPadding = paddingValues,
            extension = state.extension,
            onClickStore = state.extension.store
                ?.contact
                ?.website
                ?.takeIf { it.isNotBlank() }
                ?.let { website -> { uriHandler.openUri(website) } },
            sources = state.sources,
            incognitoMode = state.isIncognito,
            onClickSourcePreferences = onClickSourcePreferences,
            onClickUninstall = onClickUninstall,
            onClickSource = onClickSource,
            onClickUseForSource = onClickUseForSource,
            onClickIncognito = onClickIncognito,
        )
    }
}

@Composable
private fun ExtensionDetails(
    contentPadding: PaddingValues,
    extension: Extension.Loaded,
    onClickStore: (() -> Unit)?,
    sources: ImmutableList<ExtensionDetailsSourceUiModel>,
    incognitoMode: Boolean,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickUseForSource: (sourceKey: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showContentWarning by remember { mutableStateOf(false) }
    val contentWarning = extension.contentWarning.label

    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.isObsolete) {
            item {
                WarningCard(
                    text = stringResource(MR.strings.obsolete_extension_message),
                    modifier = Modifier.padding(bottom = KatariTheme.spacing.m),
                )
            }
        }

        item {
            ExtensionDetailsHeader(
                extension = extension,
                extIncognitoMode = incognitoMode,
                onClickUninstall = onClickUninstall,
                onClickAppInfo = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                    Unit
                }.takeIf { extension.isShared },
                onClickStore = onClickStore,
                onClickContentWarning = {
                    showContentWarning = true
                },
                onExtIncognitoChange = onClickIncognito,
            )
        }

        if (sources.isNotEmpty()) {
            item(key = "sources-header") {
                RowGroupHeader(stringResource(MR.strings.label_sources))
            }
        }
        rowGroupItems(items = sources, key = { source -> source.id }) { source ->
            ExtensionSourceRow(
                source = source,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickSource = onClickSource,
            )
        }

        if (extension.sourceConflicts.isNotEmpty()) {
            item(key = "source-conflicts-header") {
                RowGroupHeader(stringResource(MR.strings.ext_provided_by_other_extensions))
            }
        }
        rowGroupItems(items = extension.sourceConflicts) { conflict ->
            ExtensionSourceConflictRow(conflict = conflict, onClickUseForSource = onClickUseForSource)
        }
    }
    if (showContentWarning && contentWarning != null) {
        ContentWarningDialog(
            label = contentWarning,
            onDismissRequest = { showContentWarning = false },
        )
    }
}
