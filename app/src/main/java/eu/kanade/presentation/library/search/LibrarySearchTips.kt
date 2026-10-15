package eu.kanade.presentation.library.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Lightbulb
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shown while the library search field is open and empty: the query language (fields, comparisons, negation and
 * alternatives) is otherwise invisible, so each token here starts a query when tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibrarySearchTips(
    onInsertToken: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.large,
        shadow = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Secondary,
                )
                Text(
                    text = stringResource(MR.strings.library_search_tips_title),
                    role = TextRole.Label,
                    tone = ContentTone.Secondary,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                SearchTokens.forEach { token ->
                    AssistChip(
                        onClick = { onInsertToken(token) },
                        label = { Text(token, role = TextRole.Code) },
                    )
                }
            }
            Text(
                text = stringResource(MR.strings.library_search_tips_examples),
                role = TextRole.Supporting,
            )
        }
    }
}

// Tokens of the library query language (see LibrarySearchField and LibrarySearchParser).
private val SearchTokens = listOf("author:", "genre:", "source:", "unread>0", "-genre:", "title:\"\"", "||")
