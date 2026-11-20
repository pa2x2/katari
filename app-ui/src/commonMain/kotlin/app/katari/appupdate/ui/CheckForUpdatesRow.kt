package app.katari.appupdate.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.relativeTimeSpan
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.minutes

/**
 * The Check for updates row of whatever Katari updates from its releases: Katari itself in About, the mpv add-on in the
 * player settings. [subtitle] says what the check found, and a spinner shows while [checking].
 */
@Composable
fun CheckForUpdatesRow(subtitle: String?, checking: Boolean, onClick: () -> Unit) {
    TextPreferenceWidget(
        title = stringResource(MR.strings.check_for_updates),
        subtitle = subtitle,
        widget = {
            AnimatedVisibility(visible = checking) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
            }
        },
        onPreferenceClick = onClick,
    )
}

/** When an update check ran, for "Up to date · checked …" and "Last checked …". */
@Composable
fun checkedTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    // The relative time would say "0 minutes ago" right after a check.
    if (now - epochMillis < 1.minutes.inWholeMilliseconds) return stringResource(MR.strings.app_update_just_now)
    return relativeTimeSpan(epochMillis, now)
}
