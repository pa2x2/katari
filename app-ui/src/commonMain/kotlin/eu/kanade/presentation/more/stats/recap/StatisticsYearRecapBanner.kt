package eu.kanade.presentation.more.stats.recap

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.AutoStories
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.StoryBanner
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Points to the year recap at the top of Statistics until it's opened, in the colours its pages take by default. */
@Composable
internal fun StatisticsYearRecapBanner(title: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    StoryBanner(
        onClick = onOpen,
        modifier = modifier,
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
