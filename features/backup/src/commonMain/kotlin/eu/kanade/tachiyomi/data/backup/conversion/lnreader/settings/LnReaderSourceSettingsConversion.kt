package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import tachiyomi.i18n.*

/** LNReader's Browse settings: its discover lists and search load, which Katari has none of. */
internal fun LnReaderSettings.convertBrowseSettings(settings: LnReaderBrowseSettings) {
    leaveBehindHeld(
        settings.showAniList to LnReaderSettingLabels.SHOW_ANILIST,
        settings.showMyAnimeList to LnReaderSettingLabels.SHOW_MY_ANIME_LIST,
        settings.globalSearchConcurrency to LnReaderSettingLabels.CONCURRENT_SEARCHES,
    )
}

/**
 * LNReader's pinned plugins, for those the converted backup lists as sources, by their keys there (D088). Its language
 * filter names languages rather than Katari's codes, and one naming only other languages would hide the sources
 * brought over; its user agent is the one it sends plugins' requests with. Both are listed.
 */
internal fun LnReaderSettings.convertSourceSettings(
    sources: List<BackupSource>,
    sourceKey: (pluginId: String) -> Long,
) {
    take(PINNED_PLUGINS, stored.decode(PINNED_PLUGINS, ListSerializer(String.serializer())))?.let { pluginIds ->
        val sourceKeys = sources.mapTo(hashSetOf()) { it.sourceId }
        val pinned = pluginIds.map(sourceKey).filter { it in sourceKeys }.mapTo(linkedSetOf()) { it.toString() }
        if (pinned.isNotEmpty()) write(SourcePreferences.PINNED_SOURCES_KEY, StringSetPreferenceValue(pinned))
    }
    takeText("LANGUAGES_FILTER")?.let {
        leaveBehind(LnReaderSettingLabels.LANGUAGES, MR.strings.backup_conversion_lnreader_languages_kept)
    }
    takeText("USER_AGENT")?.let {
        leaveBehind(LnReaderSettingLabels.USER_AGENT, MR.strings.backup_conversion_lnreader_user_agent_kept)
    }
}

private const val PINNED_PLUGINS = "PINNED_PLUGINS"
