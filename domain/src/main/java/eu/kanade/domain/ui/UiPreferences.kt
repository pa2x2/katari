package eu.kanade.domain.ui

import android.text.format.DateFormat
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.util.system.logcat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    preferenceStore: PreferenceStore,
    dynamicColorAvailable: Boolean = false,
) {

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    val appTheme: Preference<AppTheme> = preferenceStore.getEnum(
        "pref_app_theme",
        if (dynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val timeFormat: Preference<TimeFormat> = preferenceStore.getEnum("app_time_format", TimeFormat.FOLLOW_LANGUAGE)

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    companion object {
        fun dateFormat(format: String, locale: Locale): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            else -> DateTimeFormatter.ofPattern(format, locale)
        }

        /**
         * The skeleton letter for the hour in [timeFormat]: "H" or "h" for a fixed clock, or "j" to let the locale
         * pick, for building formats with [DateFormat.getBestDateTimePattern].
         */
        fun hourSkeleton(timeFormat: TimeFormat, is24HourSystem: Boolean): String = when (timeFormat) {
            TimeFormat.FOLLOW_LANGUAGE -> "j"
            TimeFormat.FOLLOW_SYSTEM -> if (is24HourSystem) "H" else "h"
            TimeFormat.HOURS_24 -> "H"
            TimeFormat.HOURS_12 -> "h"
        }

        /** For formats that change shape with the clock rather than take a skeleton, such as "9 PM" against "21:00". */
        fun uses24Hours(timeFormat: TimeFormat, locale: Locale, is24HourSystem: Boolean): Boolean = when (timeFormat) {
            TimeFormat.FOLLOW_LANGUAGE -> DateFormat.getBestDateTimePattern(locale, "j").any { it == 'H' || it == 'k' }
            TimeFormat.FOLLOW_SYSTEM -> is24HourSystem
            TimeFormat.HOURS_24 -> true
            TimeFormat.HOURS_12 -> false
        }

        fun timeFormat(timeFormat: TimeFormat, locale: Locale, is24HourSystem: Boolean): DateTimeFormatter {
            if (timeFormat == TimeFormat.FOLLOW_LANGUAGE) {
                return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            }
            val hour = hourSkeleton(timeFormat, is24HourSystem)
            val pattern = DateFormat.getBestDateTimePattern(locale, "${hour}m")
            return try {
                DateTimeFormatter.ofPattern(pattern, locale)
            } catch (e: IllegalArgumentException) {
                val fallbackPattern = if (hour == "H") "HH:mm" else "h:mm a"
                logcat(LogPriority.ERROR, e) {
                    "Failed to apply best pattern ('$pattern'), falling back to '$fallbackPattern'"
                }
                DateTimeFormatter.ofPattern(fallbackPattern, locale)
            }
        }

        fun dateTimeFormat(
            dateFormat: String,
            timeFormat: TimeFormat,
            locale: Locale,
            is24HourSystem: Boolean,
        ): DateTimeFormatter {
            return DateTimeFormatterBuilder()
                .append(dateFormat(dateFormat, locale))
                .appendLiteral(' ')
                .append(timeFormat(timeFormat, locale, is24HourSystem))
                .toFormatter(locale)
        }
    }
}
