package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Source

/**
 * A source's row: its icon, name and language, the entry types it supplies and any [action]s. [below] adds a line
 * under the language, such as a warning.
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
    action: @Composable RowScope.() -> Unit = {},
) {
    val sourceLangString = LocaleHelper.getSourceDisplayName(source.lang, LocalContext.current).takeIf {
        showLanguageInContent
    }
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = sourceLangString,
        leading = { SourceIcon(source = source) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceEntryTypeIndicators(source.supportedEntryTypes)
                action()
            }
        },
        onClick = onClickItem,
        onLongClick = onLongClickItem,
        below = below,
    )
}
