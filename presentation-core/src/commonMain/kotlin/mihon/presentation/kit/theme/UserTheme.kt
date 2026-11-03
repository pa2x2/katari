package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme

/** The theme the user picked in [UiPreferences], with whether it is dark now, as [KatariTheme] applies it. */
internal class UserTheme(
    val appTheme: AppTheme,
    val isAmoled: Boolean,
    val isDark: Boolean,
)

/** The user's theme, as each platform puts a change of it into effect. */
@Composable
internal expect fun currentUserTheme(uiPreferences: UiPreferences): UserTheme
