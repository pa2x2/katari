package mihon.desktop.debug

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import eu.kanade.presentation.more.settings.screen.debug.DeviceFact
import eu.kanade.presentation.util.Screen
import mihon.core.platform.PlatformContext

/**
 * Desktop collects no logs, device facts or background work list for bug reports yet. Stands in until desktop gathers
 * its own diagnostics.
 */
@Inject
class NoDeviceDiagnostics : DeviceDiagnostics {
    override val isOffered: Boolean = false

    override suspend fun dumpLogs(context: PlatformContext) = Unit

    override suspend fun debugInfo(context: PlatformContext): String = error("Desktop collects no debug info")

    override suspend fun profileCompilationStatus(): String = error("Desktop has no startup profile")

    override fun deviceFacts(): List<DeviceFact> = emptyList()

    override fun backgroundWorkScreen(): Screen = error("Desktop lists no background work")
}
