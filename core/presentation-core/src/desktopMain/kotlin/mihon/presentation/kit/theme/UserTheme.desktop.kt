package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

// A window keeps its content through a change, so the theme follows the preferences as they change, the active
// profile's after a switch too, and the desktop's own preference while the user leaves light or dark to it.
@Composable
internal actual fun currentUserTheme(uiPreferences: UiPreferences): UserTheme {
    val appTheme by uiPreferences.appTheme.collectPreferenceAsState()
    val isAmoled by uiPreferences.themeDarkAmoled.collectPreferenceAsState()
    val themeMode by uiPreferences.themeMode.collectPreferenceAsState()
    val systemPrefersDark by rememberKatariThemeGraph().systemColorScheme.prefersDark.collectAsState()
    return UserTheme(
        appTheme = appTheme,
        isAmoled = isAmoled,
        isDark = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemPrefersDark
        },
    )
}
