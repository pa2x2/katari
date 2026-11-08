package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.content.LogoSize
import app.katari.presentation.kit.list.SettingsRow
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.util.system.LocaleHelper
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
    val sourceRows = LocalAppUiGraph.current.sourceRows
    val sourceLangString = LocaleHelper.getSourceDisplayName(source.lang, currentPlatformContext()).takeIf {
        showLanguageInContent
    }
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = sourceOrigin(sourceLangString, source.id),
        leading = { sourceRows.Icon(source = source, size = LogoSize.Row, modifier = Modifier) },
        trailing = {
            SourceEntryTypeIndicators(source.supportedEntryTypes)
            action()
        },
        onClick = onClickItem,
        onLongClick = onLongClickItem,
        below = below,
    )
}
