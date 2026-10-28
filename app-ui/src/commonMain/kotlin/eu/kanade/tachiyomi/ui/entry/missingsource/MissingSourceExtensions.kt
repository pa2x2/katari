package eu.kanade.tachiyomi.ui.entry.missingsource

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.Flow

/** The extension that would provide a source the app doesn't have, as the stores the user added list it. */
interface MissingSourceExtensions {
    /** False where extensions can't be installed; entries then don't offer their missing-source sheet. */
    val isOffered: Boolean

    /**
     * The listing of the extension that provides [sourceId]; null when no added store lists it, or none that this app
     * can install from.
     */
    fun listing(sourceId: Long): Flow<Extension.Available?>

    /** Installs [listing], reporting each step until it completes. Only called while [isOffered]. */
    fun install(listing: Extension.Available): Flow<InstallStep>
}
