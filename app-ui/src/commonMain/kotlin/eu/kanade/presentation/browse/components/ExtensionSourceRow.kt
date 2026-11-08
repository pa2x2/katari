package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.control.Switch
import app.katari.presentation.kit.list.SettingsRow
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsSourceUiModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
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
    val context = currentPlatformContext()

    SettingsRow(
        title = if (source.labelAsName) {
            source.name
        } else {
            LocaleHelper.getSourceDisplayName(source.lang, context)
        },
        modifier = modifier,
        subtitle = source.title.takeUnless { it == source.name },
        trailing = {
            SourceEntryTypeIndicators(source.supportedEntryTypes)
            if (source.hasSettings) {
                IconButton(
                    icon = MaterialSymbols.Rounded.Settings,
                    contentDescription = stringResource(MR.strings.label_settings),
                    onClick = { onClickSourcePreferences(source.id) },
                )
            }
            Switch(checked = source.enabled, onCheckedChange = null)
        },
        role = Role.Switch,
        onClick = { onClickSource(source.id) },
    )
}
