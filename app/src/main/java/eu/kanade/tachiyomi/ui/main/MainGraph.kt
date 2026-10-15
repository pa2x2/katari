package eu.kanade.tachiyomi.ui.main

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.media.EntryMediaCacheFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.domain.library.service.LibraryPreferences

interface MainGraph {
    val libraryPreferences: LibraryPreferences
    val basePreferences: BasePreferences
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val profileManager: ProfileManager
    val sourceMigrationSessionStore: SourceMigrationSessionStore
    val entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature
    val entryMediaCacheFeature: EntryMediaCacheFeature
    val entryMergeNavigationFeature: EntryMergeNavigationFeature
    val extensionManager: ExtensionManager
    val getIncognitoState: GetIncognitoState
}
