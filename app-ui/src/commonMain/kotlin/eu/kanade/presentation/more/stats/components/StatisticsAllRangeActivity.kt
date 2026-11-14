package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AllRangeEmptyActivity() {
    Column(
        modifier = Modifier.fillMaxWidth().height(StatisticsMetrics.emptyActivityHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(MR.strings.statistics_no_activity), role = TextRole.Label)
        Text(
            text = stringResource(MR.strings.statistics_all_activity_empty_hint),
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
            role = TextRole.Supporting,
        )
    }
}
