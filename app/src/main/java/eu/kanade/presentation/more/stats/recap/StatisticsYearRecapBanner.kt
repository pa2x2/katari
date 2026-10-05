package eu.kanade.presentation.more.stats.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Points to the year recap at the top of Statistics until it's opened, in the recap's own colours. */
@Composable
internal fun StatisticsYearRecapBanner(title: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val palette = RecapPalette.Default
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(palette.background, palette.backgroundEnd)))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = palette.accent)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.statistics_recap_banner_title, title),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            Text(
                text = stringResource(MR.strings.statistics_recap_banner_text),
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
        Button(
            onClick = onOpen,
            colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.background),
        ) {
            Text(stringResource(MR.strings.action_open))
        }
    }
}
