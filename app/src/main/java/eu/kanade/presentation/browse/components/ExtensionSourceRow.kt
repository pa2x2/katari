package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsSourceUiModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.control.Switch
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One of an extension's sources, switched on or off, with a shortcut to its settings when it has any. */
@Composable
internal fun ExtensionSourceRow(
    source: ExtensionDetailsSourceUiModel,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    SettingsRow(
        title = if (source.labelAsName) {
            source.name
        } else {
            LocaleHelper.getSourceDisplayName(source.lang, context)
        },
        modifier = modifier,
        subtitle = source.title.takeUnless { it == source.name },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceEntryTypeIndicators(source.supportedEntryTypes)
                if (source.hasSettings) {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Settings,
                        contentDescription = stringResource(MR.strings.label_settings),
                        onClick = { onClickSourcePreferences(source.id) },
                    )
                }
                Switch(checked = source.enabled, onCheckedChange = null)
            }
        },
        role = Role.Switch,
        onClick = { onClickSource(source.id) },
    )
}
