package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.StoryPalette
import mihon.presentation.kit.theme.StoryTheme

/** A banner in a story's colours that opens the story, such as a year recap's entry on the statistics page. */
@Composable
fun StoryBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: StoryPalette = StoryPalette.App,
    content: @Composable RowScope.() -> Unit,
) {
    StoryTheme(palette) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            modifier = modifier
                .fillMaxWidth()
                .clip(KatariTheme.shapes.large)
                .background(Brush.horizontalGradient(listOf(palette.background, palette.backgroundEnd)))
                .clickable(onClick = onClick)
                .padding(KatariTheme.spacing.l),
            content = content,
        )
    }
}
