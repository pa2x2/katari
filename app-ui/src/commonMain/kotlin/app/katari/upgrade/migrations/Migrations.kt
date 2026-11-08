package app.katari.upgrade.migrations

import app.katari.upgrade.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        SetupLibraryUpdateMigration(),
        SetupStatisticsRecapMigration(),
        TrustExtensionRepositoryMigration(),
        CategoryPreferencesCleanupMigration(),
        InstallationIdMigration(),
        VerticalNavigatorMigration(),
        ChapterTransitionMigration(),
        MangaReaderSystemBarsMigration(),
        LibraryUpdateRulesMigration(),
        SourceUpdatePausesMigration(),
        ContentWarningMigration(),
        CoverSizeMigration(),
        MigrationOptionsMigration(),
        TrackerDataCleanupMigration(),
    )
