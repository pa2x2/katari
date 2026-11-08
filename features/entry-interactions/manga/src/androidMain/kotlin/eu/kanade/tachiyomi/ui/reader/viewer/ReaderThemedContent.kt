package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.readerBackgroundIsDark

/** [content] in the dark or light theme that suits the reader's background, which the reader theme picks. */
@Composable
internal fun ReaderThemedContent(activity: ReaderActivity, content: @Composable () -> Unit) {
    val readerTheme = activity.viewModel.readerSettings.readerTheme.state.collectAsState().value.effectiveValue
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val dark = context.readerBackgroundIsDark(readerTheme, activity.uiPreferences)
    val configuration = remember(base, dark) {
        Configuration(base).apply {
            val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        }
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
