package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A filter group's header with a line under it saying what the group decides. */
@Composable
fun FilterSectionHeading(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title)
        Text(
            text = summary,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
            role = TextRole.Supporting,
        )
    }
}
