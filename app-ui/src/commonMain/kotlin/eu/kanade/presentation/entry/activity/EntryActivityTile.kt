package eu.kanade.presentation.entry.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.Card
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

@Composable
internal fun EntryActivityTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(KatariTheme.spacing.m)) {
            Text(
                text = value,
                role = TextRole.SectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(KatariTheme.spacing.xs))
            Text(text = label, role = TextRole.Supporting)
            detail?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
        }
    }
}
