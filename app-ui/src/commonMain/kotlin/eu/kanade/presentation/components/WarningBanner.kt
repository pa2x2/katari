package eu.kanade.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.WarningContainer) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.l),
            role = TextRole.Body,
            textAlign = TextAlign.Center,
        )
    }
}
