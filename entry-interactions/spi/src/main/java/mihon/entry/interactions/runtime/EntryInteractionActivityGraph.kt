package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry

/** Graph entry points of the readers and players entry interactions open, which Android constructs. */
@ContributesTo(AppScope::class)
interface EntryInteractionActivityGraph {
    val entryInteractionActivityTheme: EntryInteractionActivityTheme
    val readerBasePreferences: ReaderBasePreferences
    val securityPreferences: SecurityPreferences
    val readerSharedSettingsRegistry: ReaderSharedSettingsRegistry
}
