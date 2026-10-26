package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What an entry offers when its source isn't installed: installing the extension that provides it from a store that
 * lists it, or migrating to another source. Without a listing it says to add a store that lists it.
 */
@Composable
internal fun MissingSourceSheet(
    sourceName: String,
    listing: Extension.Available?,
    installStep: InstallStep,
    onInstall: (Extension.Available) -> Unit,
    onMigrate: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = KatariTheme.spacing.l)) {
            SheetHeader(title = stringResource(MR.strings.missing_source_title, sourceName))
            RowGroupNote(
                text = stringResource(
                    if (listing != null) MR.strings.missing_source_listed else MR.strings.missing_source_unlisted,
                ),
            )
            (installStep as? InstallStep.Error)?.let {
                RowGroupNote(text = stringResource(MR.strings.ext_install_error), tone = ContentTone.Error)
            }
            RowGroup(
                rows = listOfNotNull(
                    listing?.let {
                        {
                            SettingsRow(
                                title = stringResource(MR.strings.missing_source_install, it.name),
                                subtitle = if (installStep.isCompleted()) {
                                    stringResource(
                                        MR.strings.missing_source_install_from,
                                        it.store.name,
                                        it.versionName,
                                    )
                                } else {
                                    stringResource(MR.strings.ext_installing)
                                },
                                icon = MaterialSymbols.Rounded.Download,
                                enabled = installStep.isCompleted(),
                                onClick = { onInstall(it) },
                            )
                        }
                    },
                    onMigrate?.let {
                        {
                            SettingsRow(
                                title = stringResource(MR.strings.missing_source_migrate),
                                icon = MaterialSymbols.Rounded.SwapHoriz,
                                onClick = {
                                    onDismissRequest()
                                    it()
                                },
                            )
                        }
                    },
                ),
            )
        }
    }
}
