package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.domain.source.model.Source

/**
 * A source's row: its icon, name, language and store, the entry types it supplies and any [action]s. [below] adds a
 * line under the subtitle, such as a warning.
 */
@Composable
fun BaseSourceItem(
    source: Source,
    modifier: Modifier = Modifier,
    title: String = source.name,
    showLanguageInContent: Boolean = true,
    onClickItem: (() -> Unit)? = null,
    onLongClickItem: (() -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    action: @Composable () -> Unit = {},
) {
    val sourceLangString = LocaleHelper.getSourceDisplayName(source.lang, LocalContext.current).takeIf {
        showLanguageInContent
    }
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = sourceOrigin(sourceLangString, source.id),
        leading = { SourceIcon(source = source) },
        trailing = {
            SourceEntryTypeIndicators(source.supportedEntryTypes)
            action()
        },
        onClick = onClickItem,
        onLongClick = onLongClickItem,
        below = below,
    )
}
