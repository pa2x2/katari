package mihon.presentation.kit.theme

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.domain.ui.UiPreferences

/** Graph entry point of [KatariTheme], which reads the user's theme wherever a screen is composed. */
@ContributesTo(AppScope::class)
interface KatariThemeGraph {
    val uiPreferences: UiPreferences
}
