package app.katari.sync.ui.arrival.change.menu

import androidx.compose.runtime.Composable
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.listChange
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The feeds saved in Browse and their presets: those added, removed and edited, by the names Browse shows. */
@Composable
internal fun savedFeedChanges(changes: List<KeyChange>, lookups: ChangeLookups): List<ChangePart> {
    val presets = changes.firstNotNullOfOrNull { change ->
        change.of { lookups.sourcePreferences(it).savedFeedPresets }
    }
    val known = (presets?.let { it.before + it.after } ?: lookups.savedFeedPresets()).associateBy(SourceFeedPreset::id)
    val feeds = changes.firstNotNullOfOrNull { change -> change.of { lookups.sourcePreferences(it).savedFeeds } }
    return listOfNotNull(
        feeds?.let { (before, after) ->
            byId(stringResource(MR.strings.sync_arrived_feeds), before, after, { it.id }) { feed ->
                feed.title?.takeIf(String::isNotBlank)
                    ?: known[feed.presetId]?.displayName()
                    ?: lookups.sourceName(feed.sourceId)
            }
        },
        presets?.let { (before, after) ->
            byId(stringResource(MR.strings.sync_arrived_feed_presets), before, after, SourceFeedPreset::id) {
                it.displayName()
            }
        },
    ).flatten()
}

/** What was added, removed and edited in a list of things known by their IDs. */
@Composable
private fun <T> byId(
    name: String,
    before: List<T>,
    after: List<T>,
    id: (T) -> String,
    label: @Composable (T) -> String,
): List<ChangePart> {
    val was = before.associateBy(id)
    val now = after.associateBy(id)
    return listOfNotNull(
        listChange(name, MR.strings.sync_arrived_added, (now - was.keys).values.map { label(it) }, readsAlone = false),
        listChange(
            name,
            MR.strings.sync_arrived_removed,
            (was - now.keys).values.map {
                label(it)
            },
            readsAlone = false,
        ),
        listChange(
            name,
            MR.strings.sync_arrived_edited,
            now.filter { (key, item) -> was[key]?.let { it != item } == true }.values.map { label(it) },
            readsAlone = false,
        ),
    )
}
