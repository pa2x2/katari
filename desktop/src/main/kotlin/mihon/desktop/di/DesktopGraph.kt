package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.app.ui.host.AppProcessGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings
import mihon.desktop.application.AppRestart
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.backend.DisplayBackendPreferences
import mihon.desktop.display.size.InterfaceSizePreferences
import mihon.desktop.display.size.SessionScale
import mihon.desktop.extension.ExtensionHostGraph
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.window.WindowPreferences
import mihon.entry.interactions.child.EntryChildBindings
import mihon.entry.interactions.download.EntryDownloadBindings
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindings
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveBindings
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalBindings
import mihon.entry.interactions.media.EntryMediaBindings
import mihon.entry.interactions.merge.EntryMergeBindings
import mihon.entry.interactions.migration.EntryMigrationBindings
import mihon.entry.interactions.state.EntryStateBackupParticipantBindings
import mihon.entry.interactions.state.EntryStateMigrationParticipantBindings
import mihon.entry.interactions.tracking.EntryTrackingMergeParticipantBindings
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationParticipantBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    // Desktop hosts no merges or entry migrations yet, so it leaves out what shared features contribute to them,
    // and the features it doesn't host yet with what they contribute to backups. Reading progress needs the merge
    // feature, so its backup state is left out too. The download features also need the merge host and source access;
    // DownloadBindings stands in for them. Deleting entries needs the download and merge features; EntryRemovalBindings
    // stands in for it. Merges also decide how the library and the migration screens group entries, which entries own
    // chapters, which entry a member opens as, what the library can merge, how entries join and leave it and which
    // members a source lists for a merge it made; MergeBindings stands in for all of them, and MigrationBindings for
    // the migration feature the library and the migration screens ask what they can migrate, and for the background
    // work that runs migrations. Moving entries between profiles needs the merge and download features too;
    // ProfileBindings stands in for it. The media features carry viewer settings through migrations; MediaBindings
    // stands in for the previews among them.
    excludes = [
        EntryTrackingMergeParticipantBindings::class,
        EntryStateMigrationParticipantBindings::class,
        EntryStateBackupParticipantBindings::class,
        EntryTranslationLanguagesMigrationParticipantBindings::class,
        EntryDownloadBindings::class,
        EntryMediaBindings::class,
        EntryMergeBindings::class,
        EntryMigrationBindings::class,
        EntryDestructiveRemovalBindings::class,
        EntryProfileMoveBindings::class,
    ],
    bindingContainers = [
        ApplicationBindings::class,
        StorageBindings::class,
        PreferenceBindings::class,
        NetworkBindings::class,
        ImageBindings::class,
        LinkBindings::class,
        ClipboardBindings::class,
        MessageBindings::class,
        ShareBindings::class,
        DestinationBindings::class,
        ThemeBindings::class,
        DisplayBindings::class,
        LanguageBindings::class,
        AuthenticationBindings::class,
        ExtensionBindings::class,
        ExtensionEnvironmentBindings::class,
        SourcePreferenceBindings::class,
        MediaBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
        LibraryBindings::class,
        EntryScreenBindings::class,
        LibraryUpdateBindings::class,
        StatisticsBindings::class,
        EntryRemovalBindings::class,
        BookReaderBindings::class,
        DownloadBindings::class,
        TranslationBindings::class,
        SpeechBindings::class,
        TextRecognitionBindings::class,
        ViewerSettingsBindings::class,
        MergeBindings::class,
        MigrationBindings::class,
        WebViewBindings::class,
        TrackerBindings::class,
        EntryTrackingHostBindings::class,
        BackupBindings::class,
        AdvancedSettingsBindings::class,
        AboutBindings::class,
        DataSettingsBindings::class,
        OnboardingBindings::class,
    ],
)
interface DesktopGraph : AppUiGraph, AppProcessGraph, ExtensionHostGraph {
    val imageLoader: ImageLoader
    val windowPreferences: WindowPreferences
    val displayBackend: DisplayBackend
    val displayBackendPreferences: DisplayBackendPreferences
    val sessionScale: SessionScale
    val interfaceSizePreferences: InterfaceSizePreferences
    val appRestart: AppRestart

    // The app passes the system's folders; developer tools running the host headless pass folders of their own.
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides directories: DesktopDirectories): DesktopGraph
    }
}
