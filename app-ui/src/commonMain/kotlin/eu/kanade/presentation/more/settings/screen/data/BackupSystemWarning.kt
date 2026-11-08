package eu.kanade.presentation.more.settings.screen.data

import app.katari.core.common.platform.PlatformFacility

/**
 * A system setting known to keep backups from being created or restored properly, such as MIUI with its optimisation
 * turned off, which the backup screens warn about.
 */
@PlatformFacility
interface BackupSystemWarning {
    /** Whether the device runs with that setting now. */
    val applies: Boolean
}
