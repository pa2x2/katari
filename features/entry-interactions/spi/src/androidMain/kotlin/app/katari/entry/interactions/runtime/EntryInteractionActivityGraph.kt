package app.katari.entry.interactions.runtime

import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.entry.interactions.reader.settings.ReaderBasePreferences
import app.katari.entry.interactions.source.EntryWebViewDestination
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.tachiyomi.core.security.SecurityPreferences

/** Graph entry points of the readers and players entry interactions open, which Android constructs. */
@ContributesTo(AppScope::class)
interface EntryInteractionActivityGraph {
    val entryInteractionActivityTheme: EntryInteractionActivityTheme
    val readerBasePreferences: ReaderBasePreferences
    val securityPreferences: SecurityPreferences
    val readerSharedSettingsRegistry: ReaderSharedSettingsRegistry
    val linkOpener: LinkOpener
    val textClipboard: TextClipboard
    val entryWebViewDestination: EntryWebViewDestination
}
