package mihon.presentation.kit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import eu.kanade.domain.ui.UiPreferences

// An activity is recreated when the theme or pure black changes, so they are read as it composes; AppCompat puts the
// light or dark choice into the configuration, which isSystemInDarkTheme reads.
@Composable
internal actual fun currentUserTheme(uiPreferences: UiPreferences): UserTheme = UserTheme(
    appTheme = uiPreferences.appTheme.get(),
    isAmoled = uiPreferences.themeDarkAmoled.get(),
    isDark = isSystemInDarkTheme(),
)
