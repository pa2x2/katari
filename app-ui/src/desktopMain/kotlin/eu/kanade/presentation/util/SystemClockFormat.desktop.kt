package eu.kanade.presentation.util

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TimeFormat
import mihon.core.platform.PlatformContext
import java.util.Locale

// Desktop doesn't read the desktop environment's own clock setting yet, so "Follow system" takes the clock of the
// system's language, as Android does for apps on a phone left at its language's default.
internal actual fun PlatformContext.is24HourSystem(): Boolean = UiPreferences.uses24Hours(
    timeFormat = TimeFormat.FOLLOW_LANGUAGE,
    locale = Locale.getDefault(Locale.Category.FORMAT),
    is24HourSystem = false,
)
