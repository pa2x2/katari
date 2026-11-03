package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.runtime.Composable
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun browseLongPressPrioritySummary(
    priority: Collection<CustomPreferences.BrowseLongPressAction>,
): String {
    val labels = sanitizeBrowseLongPressActionPriority(priority).map { action ->
        stringResource(action.titleRes)
    }
    return labels.joinToString(" → ")
}
