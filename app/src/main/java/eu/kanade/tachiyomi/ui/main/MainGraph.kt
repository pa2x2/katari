package eu.kanade.tachiyomi.ui.main

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfilePendingIntent
import tachiyomi.domain.library.service.LibraryUpdateSchedule

interface MainGraph {
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val profileManager: ProfileManager
    val profilePendingIntent: ProfilePendingIntent
    val sourceMigrationSessionStore: SourceMigrationSessionStore
    val entryMergeNavigationFeature: EntryMergeNavigationFeature
    val extensionManager: ExtensionManager
    val libraryUpdateSchedule: LibraryUpdateSchedule
}
