package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shown instead of the app while migrations from the previous version run, so a slow first launch
 * after an update doesn't look like the app hanging.
 */
@Composable
fun AppMigratingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = KatariTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingIndicator()
            Text(
                text = stringResource(MR.strings.app_migrating_title),
                role = TextRole.ScreenTitle,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(MR.strings.app_migrating_summary),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
