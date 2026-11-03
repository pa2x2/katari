package eu.kanade.presentation.more.settings.screen.debug

import eu.kanade.domain.base.InstallationPreferences

/** Graph entry points of the debug info and About's version row. */
interface DebugInfoGraph {
    val installationPreferences: InstallationPreferences
    val deviceDiagnostics: DeviceDiagnostics
}
