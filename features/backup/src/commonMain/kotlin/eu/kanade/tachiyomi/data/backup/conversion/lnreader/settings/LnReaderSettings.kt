package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.APP_SETTINGS
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.BROWSE_SETTINGS
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.CHAPTER_GENERAL_SETTINGS
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.CHAPTER_READER_SETTINGS
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.LIBRARY_SETTINGS
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.NOVEL_SETTINGS_PREFIX
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.TRACKED_NOVEL_PREFIX
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.PreferenceValue
import tachiyomi.i18n.*

/**
 * An LNReader backup's app settings as Katari's (D081-D091). Only what the backup holds is written; the settings Katari
 * has nothing for are listed with LNReader's names. Per-novel chapter lists and tracker links belong to the novels and
 * are converted with them.
 *
 * @param app the app the report names for a setting Katari has nothing for.
 * @param categoryIds the ids of the categories the converted backup holds, which are LNReader's own.
 * @param sources the converted backup's sources, one per plugin its novels come from.
 * @param sourceKey the converted backup's key for a plugin's source.
 */
internal fun convertLnReaderSettings(
    stored: LnReaderStoredSettings,
    app: String,
    categoryIds: Set<Long>,
    sources: List<BackupSource>,
    sourceKey: (pluginId: String) -> Long,
): ConvertedSettings = LnReaderSettings(stored, app).run {
    take(APP_SETTINGS, stored.app)?.let(::convertAppSettings)
    convertAppTheme()
    take(LIBRARY_SETTINGS, stored.library)?.let { convertLibrarySettings(it, categoryIds) }
    take(BROWSE_SETTINGS, stored.browse)?.let(::convertBrowseSettings)
    convertSourceSettings(sources, sourceKey)
    take(CHAPTER_GENERAL_SETTINGS, stored.chapterGeneral)?.let(::convertChapterGeneralSettings)
    take(CHAPTER_READER_SETTINGS, stored.chapterReader)?.let { reader ->
        convertReaderLook(reader)
        convertCustomCode(reader, sources)
        reader.tts?.let(::convertTtsSettings)
        leaveBehindHeld(
            reader.epubLocation?.takeIf(String::isNotEmpty) to LnReaderSettingLabels.EPUB_DIRECTORY,
            reader.epubUseAppTheme to LnReaderSettingLabels.EPUB_APPLY_READER_THEME,
            reader.epubUseCustomCSS to LnReaderSettingLabels.EPUB_INCLUDE_CSS,
            reader.epubUseCustomJS to LnReaderSettingLabels.EPUB_INCLUDE_JS,
            reader.epubIncludeChapterNumber to LnReaderSettingLabels.EPUB_INCLUDE_CHAPTER_NUMBER,
        )
    }
    finish()
}

/** The settings being converted: the keys still to convert and what has come of the rest. */
internal class LnReaderSettings(val stored: LnReaderStoredSettings, private val app: String) {
    private val remaining = stored.keys.toMutableSet()
    private val preferences = mutableListOf<BackupPreference>()
    private val carried = mutableListOf<Item>()
    private val leftBehind = mutableListOf<Item>()

    /** Takes the settings object at [key] out of those still to convert; one that can't be read is listed. */
    fun <T> take(key: String, value: Result<T>?): T? {
        remaining -= key
        return value?.onFailure { leftBehind += Item(Name.Text(key), MR.strings.backup_conversion_lnreader_unreadable) }
            ?.getOrNull()
    }

    /** Takes the setting at [key], kept as text, out of those still to convert. */
    fun takeText(key: String): String? {
        remaining -= key
        return stored.text(key)
    }

    /** Takes the switch at [key] out of those still to convert; null where the backup lacks it. */
    fun takeSwitch(key: String): Boolean? = if (remaining.remove(key)) stored.isOn(key) else null

    /** Writes a Katari setting, listed as come over with [note]. */
    fun write(key: String, value: PreferenceValue, note: StringResource? = null) {
        preferences += BackupPreference(key, value)
        carried += Item(Name.Setting(key), note)
    }

    fun leaveBehind(item: Item) {
        leftBehind += item
    }

    fun leaveBehind(label: OtherAppSettingLabel, note: StringResource = MR.strings.backup_conversion_no_counterpart) {
        leftBehind += Item(label.nameIn(app), note)
    }

    /** Lists each setting whose value the backup holds, which Katari has nothing for. */
    fun leaveBehindHeld(vararg settings: Pair<Any?, OtherAppSettingLabel>) {
        settings.forEach { (value, label) -> if (value != null) leaveBehind(label) }
    }

    fun finish(): ConvertedSettings {
        remaining.forEach { key ->
            when {
                key in LnReaderSettingLabels.IGNORED -> Unit
                HANDLED_WITH_NOVELS.any(key::startsWith) -> Unit
                LnReaderSettingLabels.IGNORED_PREFIXES.any(key::startsWith) -> Unit
                key in LnReaderSettingLabels.NO_COUNTERPART -> leaveBehind(
                    LnReaderSettingLabels.NO_COUNTERPART.getValue(key),
                )
                // A key of a version the table doesn't know, which the report names by its key (D048)
                else -> leftBehind += Item(Name.Text(key), MR.strings.backup_conversion_unknown_setting)
            }
        }
        return ConvertedSettings(preferences, carried, leftBehind)
    }

    private companion object {
        // Each novel's chapter list and tracker links, which come over with the novel
        val HANDLED_WITH_NOVELS = listOf(NOVEL_SETTINGS_PREFIX, TRACKED_NOVEL_PREFIX)
    }
}
