package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.defaultHomeScreenTabs
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*

/**
 * LNReader's general, appearance, library update, download and backup settings. Its update intervals, backup intervals
 * and date formats are choices Katari offers too, so they carry as they are.
 */
internal fun LnReaderSettings.convertAppSettings(settings: LnReaderAppSettings) {
    convertNavigationTabs(settings)
    settings.dateFormat?.let { format ->
        val pattern = DATE_FORMATS[format]
        if (pattern == null) {
            leaveBehind(Item(Name.Setting(DATE_FORMAT_KEY), MR.strings.backup_conversion_lnreader_unreadable))
        } else {
            write(DATE_FORMAT_KEY, StringPreferenceValue(pattern))
        }
    }
    settings.relativeTimestamps?.let { write("relative_time_v2", BooleanPreferenceValue(it)) }
    convertUpdateSkipRules(settings)
    settings.automaticLibraryUpdateIntervalHours?.let {
        write("pref_library_update_interval_key", IntPreferenceValue(it))
    }
    settings.automaticBackupIntervalHours?.let { write("backup_interval", IntPreferenceValue(it)) }
    settings.downloadNewChapters?.let { write("download_new", BooleanPreferenceValue(it)) }
    settings.refreshNovelMetadata?.let { write("auto_update_metadata", BooleanPreferenceValue(it)) }
    settings.defaultChapterSort?.let(::convertDefaultChapterSort)
    leaveBehindHeld(
        settings.disableHapticFeedback to LnReaderSettingLabels.DISABLE_HAPTIC_FEEDBACK,
        settings.disableLoadingAnimations to LnReaderSettingLabels.DISABLE_LOADING_ANIMATIONS,
        settings.chapterDownloadCooldownMs to LnReaderSettingLabels.CHAPTER_DOWNLOAD_COOLDOWN,
        settings.timeTrackingEnabled to LnReaderSettingLabels.ENABLE_TIME_TRACKING,
        settings.inactivityTimeoutMs to LnReaderSettingLabels.INACTIVITY_TIMEOUT,
        settings.showLabelsInNav to LnReaderSettingLabels.ALWAYS_SHOW_NAV_LABELS,
        settings.useFabForContinueReading to LnReaderSettingLabels.USE_FAB,
        settings.hideBackdrop to LnReaderSettingLabels.HIDE_BACKDROP,
        settings.useLibraryFAB to LnReaderSettingLabels.LIBRARY_RESUME_BUTTON,
        settings.updateLibraryOnLaunch to LnReaderSettingLabels.UPDATE_LIBRARY_ON_LAUNCH,
        settings.automaticBackupDirectoryUri?.takeIf(String::isNotEmpty) to
            LnReaderSettingLabels.AUTOMATIC_BACKUP_LOCATION,
    )
}

/** LNReader's theme mode and pure black, kept under keys of their own (D088). Its themes aren't backed up. */
internal fun LnReaderSettings.convertAppTheme() {
    takeText("THEME_MODE")?.let { mode ->
        val katariMode = ThemeMode.entries.find { it.name.equals(mode, ignoreCase = true) }
        if (katariMode == null) {
            leaveBehind(
                Item(Name.Setting(UiPreferences.THEME_MODE_KEY), MR.strings.backup_conversion_lnreader_unreadable),
            )
        } else {
            write(UiPreferences.THEME_MODE_KEY, StringPreferenceValue(katariMode.name))
        }
    }
    takeSwitch("AMOLED_BLACK")?.let { write("pref_theme_dark_amoled_key", BooleanPreferenceValue(it)) }
}

/** LNReader's switches for its History and Updates tabs, as Katari's one set of tabs. */
private fun LnReaderSettings.convertNavigationTabs(settings: LnReaderAppSettings) {
    if (settings.showHistoryTab == null && settings.showUpdatesTab == null) return
    val hidden = listOfNotNull(
        HomeScreenTabs.History.name.takeIf { settings.showHistoryTab == false },
        HomeScreenTabs.Updates.name.takeIf { settings.showUpdatesTab == false },
    )
    write(CustomPreferences.HOME_SCREEN_TABS_KEY, StringSetPreferenceValue(defaultHomeScreenTabs() - hidden.toSet()))
}

/**
 * LNReader's three skip rules as Katari's set, written only when one is on, and always with Katari's own rule for
 * entries outside their release period (D085). The old rule to update only ongoing novels is Katari's rule for
 * completed ones, which still updates novels on hiatus or of an unknown status.
 */
private fun LnReaderSettings.convertUpdateSkipRules(settings: LnReaderAppSettings) {
    val rules = listOfNotNull(
        LibraryPreferences.SKIP_COMPLETED.takeIf {
            settings.smartUpdateSkipCompleted == true || settings.onlyUpdateOngoingNovels == true
        },
        LibraryPreferences.SKIP_NOT_STARTED.takeIf { settings.smartUpdateSkipUnstarted == true },
        LibraryPreferences.SKIP_UNSEEN.takeIf { settings.smartUpdateSkipWithUnread == true },
    )
    if (rules.isEmpty()) return
    write(
        LibraryPreferences.UPDATE_SKIP_RULES_PREF_KEY,
        StringSetPreferenceValue(rules.toSet() + LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD),
        MR.strings.backup_conversion_lnreader_only_ongoing.takeIf {
            settings.onlyUpdateOngoingNovels == true && settings.smartUpdateSkipCompleted != true
        },
    )
}

private fun LnReaderSettings.convertDefaultChapterSort(value: String) {
    val order = LnReaderChapterOrder.of(value)
    val sorting = order?.sorting
    when {
        order == null ->
            leaveBehind(Item(Name.Setting(CHAPTER_SORT_KEY), MR.strings.backup_conversion_lnreader_unreadable))
        sorting == null ->
            leaveBehind(Item(Name.Setting(CHAPTER_SORT_KEY), MR.strings.backup_conversion_lnreader_sort_read_time))
        else -> {
            write(CHAPTER_SORT_KEY, LongPreferenceValue(sorting))
            write("default_chapter_sort_by_ascending_or_descending", LongPreferenceValue(order.direction))
        }
    }
}

private const val DATE_FORMAT_KEY = "app_date_format"
private const val CHAPTER_SORT_KEY = "default_chapter_sort_by_source_or_number"

// LNReader's DateFormat and the pattern Katari's date format setting offers for it; default is the locale's
private val DATE_FORMATS = mapOf(
    "default" to "",
    "MM/DD/YY" to "MM/dd/yy",
    "DD/MM/YY" to "dd/MM/yy",
    "YYYY-MM-DD" to "yyyy-MM-dd",
    "DD MMM YYYY" to "dd MMM yyyy",
    "MMM DD, YYYY" to "MMM dd, yyyy",
)
