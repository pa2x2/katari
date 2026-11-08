package app.katari.presentation.kit.theme

import kotlinx.coroutines.flow.StateFlow

/** The desktop's own light or dark preference, which the theme follows when the user leaves it to the system. */
interface SystemColorScheme {
    /** Whether the desktop prefers dark colours; false where it states no preference or can't be asked. */
    val prefersDark: StateFlow<Boolean>
}
