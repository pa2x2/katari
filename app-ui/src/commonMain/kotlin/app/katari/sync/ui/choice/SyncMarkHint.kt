package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * A hint atop the first Settings page with sync marks that a linked device shows: what the mark after a setting's
 * title says and how its choice opens. It shows until dismissed, or until a mark's choice is opened, and never again.
 * Null where the page has no mark or the hint was seen.
 */
@Composable
internal fun syncMarkHint(items: List<Preference>): Preference.PreferenceItem.CustomPreference? {
    val seenPreference = LocalAppUiGraph.current.syncPreferences.markHintSeen
    val seen by seenPreference.collectAsState()
    val marks = rememberSettingSyncMarks()
    if (seen || marks == null || items.none { it.hasMarkedRow(marks) }) return null
    return Preference.PreferenceItem.CustomPreference(title = "", framed = false) {
        NoticeCard(
            text = stringResource(platformPresentation.syncMarkHint),
            tone = NoticeTone.Info,
            icon = MaterialSymbols.Rounded.CloudSync,
            dismissLabel = stringResource(MR.strings.action_got_it),
            onDismiss = { seenPreference.set(true) },
        )
    }
}

private fun Preference.hasMarkedRow(marks: SettingSyncMarks): Boolean = when (this) {
    is Preference.PreferenceGroup -> preferenceItems.any { it.hasMarkedRow(marks) }
    is Preference.PreferenceItem<*, *> -> storedPreference?.let(marks::has) == true
}
