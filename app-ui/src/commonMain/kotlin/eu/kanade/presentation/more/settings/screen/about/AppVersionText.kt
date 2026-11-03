package eu.kanade.presentation.more.settings.screen.about

import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.ui.buildinfo.AppBuild
import mihon.app.ui.buildinfo.AppBuildType
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/** @param buildDateTimeFormat appends the build time in this format, or null to leave it out. */
fun AppBuild.versionText(buildDateTimeFormat: DateTimeFormatter? = null): String {
    return when (type) {
        AppBuildType.Debug -> {
            "Debug $commitSha".let {
                if (buildDateTimeFormat != null) {
                    "$it (${formattedBuildTime(buildDateTimeFormat)})"
                } else {
                    it
                }
            }
        }
        AppBuildType.Preview -> {
            "Beta r$commitCount".let {
                if (buildDateTimeFormat != null) {
                    "$it ($commitSha, ${formattedBuildTime(buildDateTimeFormat)})"
                } else {
                    "$it ($commitSha)"
                }
            }
        }
        AppBuildType.Stable -> {
            "Stable $versionName".let {
                if (buildDateTimeFormat != null) {
                    "$it (${formattedBuildTime(buildDateTimeFormat)})"
                } else {
                    it
                }
            }
        }
    }
}

fun AppBuild.formattedBuildTime(dateTimeFormat: DateTimeFormatter): String {
    return try {
        Instant.parse(buildTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toDateTimestampString(dateTimeFormat)
    } catch (_: Exception) {
        buildTime
    }
}
