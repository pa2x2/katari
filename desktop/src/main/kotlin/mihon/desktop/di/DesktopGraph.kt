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
import mihon.desktop.work.WorkCloseGuard
import mihon.entry.interactions.download.EntryDownloadBindings
import mihon.entry.interactions.host.AppEntryHostBindings
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindings
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveBindings
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalBindings
import mihon.entry.interactions.media.EntryMediaBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    // Desktop doesn't host downloads, deleting entries, moving them between profiles or the media features yet, so
    // DownloadBindings, EntryRemovalBindings, ProfileBindings and MediaBindings stand in for what screens ask of them.
    // The download features need the source access resolver only Android binds, and deleting and moving entries
    // need the download features. Without the download and media features, desktop backups and migrations carry no
    // download settings, viewer settings, playback preferences or intro-time matches.
    excludes = [
        EntryDownloadBindings::class,
        EntryMediaBindings::class,
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
        AppEntryHostBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
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
    val workCloseGuard: WorkCloseGuard

    // The app passes the system's folders; developer tools running the host headless pass folders of their own.
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides directories: DesktopDirectories): DesktopGraph
    }
}
