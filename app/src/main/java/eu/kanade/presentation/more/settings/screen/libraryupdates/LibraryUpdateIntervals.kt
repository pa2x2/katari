package eu.kanade.presentation.more.settings.screen.libraryupdates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Hours between automatic updates that the library and its categories can choose from, with their labels. */
@Composable
@ReadOnlyComposable
internal fun libraryUpdateIntervals(): Map<Int, String> = mapOf(
    12 to stringResource(MR.strings.update_12hour),
    24 to stringResource(MR.strings.update_24hour),
    48 to stringResource(MR.strings.update_48hour),
    72 to stringResource(MR.strings.update_72hour),
    168 to stringResource(MR.strings.update_weekly),
)

@Composable
@ReadOnlyComposable
internal fun libraryUpdateIntervalLabel(hours: Int): String {
    return libraryUpdateIntervals()[hours] ?: stringResource(MR.strings.update_never)
}
