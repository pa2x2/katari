package eu.kanade.presentation.more.stats.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.AutoStories
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.StoryPalette
import mihon.presentation.kit.theme.StoryTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Points to the year recap at the top of Statistics until it's opened, in the colours its pages take by default. */
@Composable
internal fun StatisticsYearRecapBanner(title: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val palette = StoryPalette.App
    StoryTheme(palette) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            modifier = modifier
                .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s)
                .fillMaxWidth()
                .clip(KatariTheme.shapes.large)
                .background(Brush.horizontalGradient(listOf(palette.background, palette.backgroundEnd)))
                .clickable(onClick = onOpen)
                .padding(KatariTheme.spacing.l),
        ) {
            Icon(MaterialSymbols.Rounded.AutoStories, contentDescription = null, tone = ContentTone.Primary)
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.statistics_recap_banner_title, title),
                    role = TextRole.SectionTitle,
                )
                Text(text = stringResource(MR.strings.statistics_recap_banner_text), role = TextRole.Supporting)
            }
            Button(label = stringResource(MR.strings.action_open), onClick = onOpen)
        }
    }
}
