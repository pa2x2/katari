package eu.kanade.presentation.more.settings.screen.debug

import app.katari.domain.app.AppIdentity
import app.katari.ui.buildinfo.AppBuild
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.InstallationPreferences
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.SourceConflict
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * The part of a bug report that reads the same on every platform: the build, the installation and the extensions that
 * misbehave. Each platform's [DeviceDiagnostics] adds what it knows about the device. The extensions are only reached
 * when a report lists them, since the crash screen's process otherwise has no reason to load them.
 */
@Inject
class AppDebugInfo(
    private val appIdentity: AppIdentity,
    private val appBuild: AppBuild,
    private val installationPreferences: InstallationPreferences,
    private val extensions: Lazy<ExtensionManagement>,
) {
    /** The build and installation, then [deviceLines], then the time it was made. */
    fun summary(deviceLines: List<String>): String {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        return buildList {
            add("App ID: ${appIdentity.applicationId}")
            add(
                "App version: ${appBuild.versionName} " +
                    "(${appBuild.commitSha}, ${appBuild.versionCode}, ${appBuild.buildTime})",
            )
            add("Installation ID: ${installationPreferences.installationId.get()}")
            addAll(deviceLines)
            add("Current time: ${now.toLocalDateTime(tz)}${tz.offsetAt(now)}")
        }
            .joinToString("\n")
    }

    /** The installed extensions that are outdated, orphaned, overridden or not loaded; null when there are none. */
    suspend fun problematicExtensions(): String? {
        val management = extensions.value
        val availableExtensions = management.available.first().associateBy { it.pkgName }

        val outdatedInfoList = management.loaded.first()
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = availableExtensions[it.pkgName]
                val hasUpdate = (availableExtension?.versionCode ?: 0) > it.versionCode

                if (!hasUpdate && !it.isObsolete && it.sourceConflicts.isEmpty()) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Orphaned: ${it.isObsolete}
                      Sources provided elsewhere: ${it.sourceConflicts.describe().ifEmpty { "none" }}
                """.trimIndent()
            }

        val notLoadedInfoList = management.notLoaded.first()
            .sortedBy { it.name }
            .map { extension ->
                buildString {
                    appendLine("- ${extension.name}")
                    appendLine("  Installed: ${extension.versionName} (lib ${extension.libVersion ?: "?"})")
                    append("  Not loaded: ${extension.reason.description}")

                    val reason = extension.reason
                    if (reason is Extension.NotLoaded.Reason.Failed) {
                        appendLine()
                        append(reason.stackTrace.trimEnd().prependIndent("  "))
                    }
                }
            }

        val extensionInfoList = outdatedInfoList + notLoadedInfoList

        return if (extensionInfoList.isNotEmpty()) {
            (listOf("Problematic extensions:") + extensionInfoList)
                .joinToString("\n")
        } else {
            null
        }
    }
}

private val Extension.NotLoaded.Reason.description: String
    get() = when (this) {
        is Extension.NotLoaded.Reason.Untrusted -> "Untrusted"
        Extension.NotLoaded.Reason.Filtered -> "Filtered by content warning"
        Extension.NotLoaded.Reason.Unsigned -> "Unsigned"
        is Extension.NotLoaded.Reason.UnsupportedLibVersion -> "Unsupported lib version $libVersionName"
        Extension.NotLoaded.Reason.Torrent -> "Torrent extension"
        Extension.NotLoaded.Reason.Malformed -> "Malformed"
        Extension.NotLoaded.Reason.Unreadable -> "Unreadable file"
        Extension.NotLoaded.Reason.Damaged -> "File changed since it was installed"
        is Extension.NotLoaded.Reason.Failed -> "Failed ($message)"
        is Extension.NotLoaded.Reason.SourceConflicts -> "Every source provided elsewhere (${conflicts.describe()})"
    }

private fun List<SourceConflict>.describe(): String {
    return joinToString { "${it.sourceName} ${it.sourceLang} by ${it.providedBy ?: "a source with the same key"}" }
}
