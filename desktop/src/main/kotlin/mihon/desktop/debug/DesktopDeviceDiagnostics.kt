package mihon.desktop.debug

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.debug.AppDebugInfo
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import eu.kanade.presentation.more.settings.screen.debug.DeviceFact
import eu.kanade.presentation.util.Screen
import logcat.LogPriority
import mihon.core.platform.PlatformContext
import mihon.desktop.logging.DesktopLogFiles
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.storage.DocumentUri
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * The log file and the running system for bug reports. Desktop has no share sheet, so logs are saved into a file the
 * user picks; it compiles no startup profile and keeps no list of background work yet.
 */
@Inject
class DesktopDeviceDiagnostics(
    private val appDebugInfo: AppDebugInfo,
    private val logFiles: DesktopLogFiles,
    private val operatingSystem: DesktopOperatingSystem,
    private val appMessages: AppMessages,
) : DeviceDiagnostics {
    override val savesLogsToPickedDocument: Boolean = true

    override suspend fun dumpLogs(context: PlatformContext, exception: Throwable?) {
        error("Desktop saves dumped logs into a file the user picks")
    }

    override suspend fun dumpLogsToDocument(document: DocumentUri, exception: Throwable?) {
        try {
            val problematicExtensions = appDebugInfo.problematicExtensions()
            withIOContext {
                Files.newBufferedWriter(Path.of(URI(document.value))).use { out ->
                    out.write(appDebugInfo.summary(deviceLines()) + "\n\n")
                    problematicExtensions?.let { out.write("$it\n\n") }
                    exception?.let { out.write("$it\n\n") }
                    logFiles.existing().forEach { log -> Files.newBufferedReader(log).use { it.transferTo(out) } }
                }
            }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't save the logs" }
            appMessages.show("Failed to get logs")
        }
    }

    override suspend fun debugInfo(context: PlatformContext): String = appDebugInfo.summary(deviceLines())

    override val reportsProfileCompilation: Boolean = false

    override suspend fun profileCompilationStatus(): String = error("Desktop has no startup profile")

    override fun deviceFacts(): List<DeviceFact> = listOf(
        DeviceFact(
            title = "Operating system",
            value = "${operatingSystem.displayName} ${System.getProperty(
                "os.version",
            )} (${System.getProperty("os.arch")})",
        ),
        DeviceFact(
            title = "Java runtime",
            value = "${System.getProperty("java.vm.name")} ${System.getProperty("java.runtime.version")}",
        ),
    )

    override val listsBackgroundWork: Boolean = false

    override fun backgroundWorkScreen(): Screen = error("Desktop lists no background work")

    private fun deviceLines(): List<String> = deviceFacts().map { "${it.title}: ${it.value}" }
}
