package app.katari.sync.ui.arrival.change.named

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.setChanges
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.layout.label
import eu.kanade.presentation.more.stats.layout.statisticsLayoutTab
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.preference.StoredValuesPreferenceStore
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.minutes

/** Statistics' settings, changed in its Customize sheet and its recaps. */
internal val statisticsNamedSettings = listOf(
    valueSetting(MR.strings.statistics_daily_goal, { StatisticsPreferences(it).dailyGoalMinutes }) { minutes ->
        val duration = rememberStatisticsDurationFormatter()
        if (minutes ==
            0
        ) {
            stringResource(MR.strings.statistics_goal_off)
        } else {
            duration(minutes.minutes.inWholeMilliseconds)
        }
    },
    switchSetting(MR.strings.statistics_recap_include_nsfw) { StatisticsPreferences(it).recapIncludeNsfw },
    namedSetting(MR.strings.statistics_recap_hidden_titles, { StatisticsPreferences(it).recapHiddenEntries }) {
            name,
            value,
            _,
        ->
        setChanges(
            name,
            value.before,
            value.after,
            { recapTitle(it) },
            added = MR.strings.sync_arrived_hid,
            removed = MR.strings.sync_arrived_showed,
            readsAlone = true,
        )
    },
    familySetting(
        prefix = StatisticsPreferences(StoredValuesPreferenceStore(emptyMap())).cardLayout("").key(),
        item = { tab ->
            tab.takeIf { it == statisticsLayoutTab(null) }
                ?: EntryType.entries.find { it.name == tab }?.name
        },
        name = { tab ->
            val type = EntryType.entries.find { it.name == tab }
            stringResource(
                MR.strings.sync_setting_statistics_cards,
                type?.let { stringResource(it.entryTypePresentation().displayNameLabel) }
                    ?: stringResource(MR.strings.label_overview_section),
            )
        },
        preference = { tab -> { StatisticsPreferences(it).cardLayout(tab) } },
    ) { name, value ->
        val before = StatisticsCardLayout.decode(value.before)
        val after = StatisticsCardLayout.decode(value.after)
        val hidden = setChanges(
            name,
            before.hidden,
            after.hidden,
            { card: StatisticsCard -> stringResource(card.label()) },
            added = MR.strings.sync_arrived_hid,
            removed = MR.strings.sync_arrived_showed,
            readsAlone = true,
        )
        val reordered = stringResource(MR.strings.sync_arrived_reordered)
        hidden +
            listOfNotNull(
                ChangePart(name, reordered, reordered, readsAlone = false).takeIf {
                    before.order !=
                        after.order
                },
            )
    },
)

/** A title hidden from a year's recap, stored as "year:entry id", by its title here and the recap's year. */
@Composable
private fun recapTitle(item: String): String {
    val year = item.substringBefore(':')
    val id = item.substringAfter(':').toLongOrNull()
    val lookups = LocalAppUiGraph.current.changeLookups
    val title by produceState<String?>(null, id) { value = id?.let { lookups.entryTitle(it) } }
    return stringResource(MR.strings.sync_arrived_recap_title, title ?: item, year)
}
