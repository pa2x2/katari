package eu.kanade.tachiyomi.ui.main

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfilePendingIntent

interface MainGraph {
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val profileManager: ProfileManager
    val profilePendingIntent: ProfilePendingIntent
    val sourceMigrationSessionStore: SourceMigrationSessionStore
    val entryMergeNavigationFeature: EntryMergeNavigationFeature
}
