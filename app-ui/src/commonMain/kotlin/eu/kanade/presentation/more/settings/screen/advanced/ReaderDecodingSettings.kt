package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference
import mihon.core.common.platform.PlatformFacility

/**
 * How the platform's readers decode pages, such as loading images progressively or with hardware bitmaps, which the
 * advanced settings tune in their reader group. Where the platform has no such readers, the group is hidden.
 */
@PlatformFacility
interface ReaderDecodingSettings {
    val isOffered: Boolean

    /** The reader group's rows, with whatever pickers they open. Only composed while [isOffered]. */
    @Composable
    fun preferenceItems(): List<Preference.PreferenceItem<out Any, out Any>>
}
