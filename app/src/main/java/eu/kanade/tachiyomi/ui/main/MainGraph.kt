package eu.kanade.tachiyomi.ui.main

import app.katari.entry.interactions.merge.EntryMergeNavigationFeature
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.core.ProfilePendingIntent
import app.katari.sourcemigration.session.SourceMigrationSessionStore
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences

interface MainGraph {
    val uiPreferences: UiPreferences
    val securityPreferences: SecurityPreferences
    val profileManager: ProfileManager
    val profilePendingIntent: ProfilePendingIntent
    val sourceMigrationSessionStore: SourceMigrationSessionStore
    val entryMergeNavigationFeature: EntryMergeNavigationFeature
}
