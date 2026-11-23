package app.katari.sync.ui.arrival.change.menu

import androidx.compose.runtime.Composable
import app.katari.core.platform.currentPlatformContext
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.setChanges
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Browse's source languages and the sources pinned or hidden, by the names Browse shows. */
@Composable
internal fun sourcesChanges(changes: List<KeyChange>, lookups: ChangeLookups): List<ChangePart> {
    val context = currentPlatformContext()
    return changes.flatMap { change ->
        change.of { lookups.sourcePreferences(it).enabledLanguages }?.let {
            return@flatMap setChanges(
                name = stringResource(MR.strings.sync_arrived_source_languages),
                before = it.before,
                after = it.after,
                label = { language -> LocaleHelper.getSourceDisplayName(language, context) },
            )
        }
        change.of { lookups.sourcePreferences(it).disabledSources }?.let {
            val names = sourceNames(it.before + it.after, lookups)
            return@flatMap setChanges(
                name = stringResource(MR.strings.sync_arrived_hidden_sources),
                before = it.before,
                after = it.after,
                label = { source -> names.getValue(source) },
                added = MR.strings.sync_arrived_hid,
                removed = MR.strings.sync_arrived_unhid,
                readsAlone = true,
            )
        }
        change.of { lookups.sourcePreferences(it).pinnedSources }?.let {
            val names = sourceNames(it.before + it.after, lookups)
            return@flatMap setChanges(
                name = stringResource(MR.strings.sync_arrived_pinned_sources),
                before = it.before,
                after = it.after,
                label = { source -> names.getValue(source) },
                added = MR.strings.sync_arrived_pinned,
                removed = MR.strings.sync_arrived_unpinned,
                readsAlone = true,
            )
        }
        emptyList()
    }
}

/**
 * The names of [sources], stored by their keys as text, with the language after a name two of them share, as Browse
 * tells such sources apart by the language it lists them under.
 */
@Composable
private fun sourceNames(sources: Set<String>, lookups: ChangeLookups): Map<String, String> {
    val context = currentPlatformContext()
    val names = sources.associateWith { source -> source.toLongOrNull()?.let(lookups::sourceName) ?: source }
    val shared = names.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return names.mapValues { (source, name) ->
        val language = source.toLongOrNull()?.let(lookups::sourceLanguage)
        if (name in shared && language != null) {
            stringResource(
                MR.strings.sync_arrived_source_language,
                name,
                LocaleHelper.getSourceDisplayName(language, context),
            )
        } else {
            name
        }
    }
}
