package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import eu.kanade.tachiyomi.data.backup.conversion.mihon.convertMihonSettings
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.PreferenceValue
import tachiyomi.i18n.*

/**
 * An Aniyomi-family backup's app settings as Katari's. Aniyomi keeps Mihon's settings and adds its own: those it adds
 * become Katari's, or Mihon's where Katari's restore or Mihon's conversion takes it from there; the ones Katari has
 * nothing for are listed with Aniyomi's names (D075). Only what the backup holds is written (D074).
 *
 * @param app the app the report names for a setting Katari has nothing for.
 * @param animeCategoryId the converted backup's id for an anime category's id; null for one the backup doesn't list.
 * @param animeSourceKey the converted backup's key for an anime source's id.
 */
internal fun convertAniyomiSettings(
    backedUp: List<BackupPreference>,
    app: String,
    animeCategoryId: (Long) -> Long?,
    animeSourceKey: (Long) -> Long,
): ConvertedSettings = AniyomiSettings(backedUp, app).run {
    convertLibrarySettings(animeCategoryId)
    convertSourceSettings(animeSourceKey)
    convertTheme()
    convertPlayerSettings()
    convertSubtitleSettings()
    finish()
}

/** The settings being converted: what is still to convert and what has come of the rest. */
internal class AniyomiSettings(backedUp: List<BackupPreference>, val app: String) {
    private val remaining = backedUp.associateByTo(linkedMapOf()) { it.key }

    /** Aniyomi's settings as Mihon keeps them, which Mihon's conversion turns into Katari's. */
    private val asMihon = mutableListOf<BackupPreference>()
    private val preferences = mutableListOf<BackupPreference>()
    private val carried = mutableListOf<Item>()
    private val leftBehind = mutableListOf<Item>()

    /** Takes the setting at [key] out of those still to convert. */
    fun take(key: String): PreferenceValue? = remaining.remove(key)?.value

    /** The setting at [key] without taking it, for a setting the report still lists that informs another. */
    fun peek(key: String): PreferenceValue? = remaining[key]?.value

    /** Writes a Katari setting, listed as come over with [note]. */
    fun write(key: String, value: PreferenceValue, note: StringResource? = null) {
        writeUnlisted(key, value)
        carried += Item(Name.Setting(key), note)
    }

    fun writeUnlisted(key: String, value: PreferenceValue) {
        preferences += BackupPreference(key, value)
    }

    /** Hands a setting to Mihon's conversion under Mihon's [key]. */
    fun writeAsMihon(key: String, value: PreferenceValue) {
        asMihon += BackupPreference(key, value)
    }

    fun leaveBehind(item: Item) {
        leftBehind += item
    }

    /** Lists the Aniyomi setting at [key], which Katari has nothing for, as left behind. */
    fun leaveBehindUnmatched(key: String) {
        leftBehind += Item(label(key).nameIn(app), MR.strings.backup_conversion_no_counterpart)
    }

    /**
     * Where Katari keeps one setting for both of Aniyomi's kinds, the anime one is kept (D062): [toMihon] gives the
     * anime value as Mihon keeps the manga one at [mangaKey], or null when Katari can't hold it, which the caller
     * lists. The manga value is listed as left behind unless it's the same. A backup with only the manga value keeps
     * it.
     */
    fun preferAnime(mangaKey: String, animeKey: String, toMihon: (PreferenceValue) -> PreferenceValue?) {
        val anime = take(animeKey) ?: return
        val converted = toMihon(anime)
        val manga = take(mangaKey)
        if (manga != null && manga != converted) {
            // Mihon's conversion knows what Katari calls the manga setting
            convertMihonSettings(listOf(BackupPreference(mangaKey, manga)), app).carried.forEach {
                leftBehind += Item(it.name, MR.strings.backup_conversion_aniyomi_manga_value)
            }
        }
        if (converted != null) writeAsMihon(mangaKey, converted)
    }

    fun finish(): ConvertedSettings {
        remaining.values.forEach { preference ->
            val key = preference.key
            when {
                key in AniyomiSettingLabels.IGNORED -> Unit
                key in AniyomiSettingLabels.NO_COUNTERPART -> leaveBehindUnmatched(key)
                key.startsWith(AniyomiSettingLabels.ANIME_LIBRARY_TRACKED_FILTER_PREFIX) ->
                    leftBehind += Item(
                        AniyomiSettingLabels.ANIME_LIBRARY_TRACKED_FILTER.nameIn(app),
                        MR.strings.backup_conversion_no_trackers,
                    )
                else -> asMihon += preference
            }
        }
        val mihon = convertMihonSettings(asMihon, app)
        return ConvertedSettings(
            preferences = mihon.preferences + preferences,
            carried = mihon.carried + carried,
            leftBehind = mihon.leftBehind + leftBehind,
        )
    }

    private fun label(key: String): OtherAppSettingLabel = AniyomiSettingLabels.NO_COUNTERPART.getValue(key)
}
