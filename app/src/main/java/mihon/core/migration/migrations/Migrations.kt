package mihon.core.migration.migrations

import mihon.core.migration.Migration

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
    )
