package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.app.ui.host.AppProcessGraph
import mihon.core.common.web.ChallengePage
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings
import mihon.desktop.appearance.PreferenceAppLocaleStore
import mihon.desktop.application.AppRestart
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.backend.DisplayBackendPreferences
import mihon.desktop.display.size.InterfaceSizePreferences
import mihon.desktop.display.size.SessionScale
import mihon.desktop.extension.ExtensionHostGraph
import mihon.desktop.extension.dialog.WindowExtensionDialogs
import mihon.desktop.home.HomeDestinations
import mihon.desktop.notification.DesktopNotifications
import mihon.desktop.player.WindowAnimePlayer
import mihon.desktop.presence.AppPresence
import mihon.desktop.profile.ActiveProfileLock
import mihon.desktop.profile.ProfileRelock
import mihon.desktop.security.signin.SignInPrompts
import mihon.desktop.security.signin.SystemSignIn
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.window.OpenWindow
import mihon.desktop.window.WindowPreferences
import mihon.desktop.work.PersistedWorkResume
import mihon.desktop.work.WorkCloseGuard
import mihon.entry.interactions.host.AppEntryHostBindings
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindings
import mihon.extension.runtime.jvm.application.WindowActivity
import mihon.feature.appupdate.AppUpdateController
import mihon.presentation.kit.files.SystemFileChooser
import mihon.webview.jcef.runtime.JcefRuntime

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        ApplicationBindings::class,
        StorageBindings::class,
        PreferenceBindings::class,
        NetworkBindings::class,
        PowerBindings::class,
        ImageBindings::class,
        LinkBindings::class,
        ClipboardBindings::class,
        MessageBindings::class,
        ShareBindings::class,
        FileChooserBindings::class,
        ThemeBindings::class,
        DisplayBindings::class,
        LanguageBindings::class,
        AuthenticationBindings::class,
        ExtensionBindings::class,
        ExtensionEnvironmentBindings::class,
        SourcePreferenceBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        AppEntryHostBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
        EntryScreenBindings::class,
        LibraryUpdateBindings::class,
        StatisticsBindings::class,
        DownloadWorkBindings::class,
        TranslationBindings::class,
        TextRecognitionBindings::class,
        ViewerSettingsBindings::class,
        MigrationBindings::class,
        WebViewBindings::class,
        PlayerBindings::class,
        ReaderBindings::class,
        TrackerBindings::class,
        EntryTrackingHostBindings::class,
        BackupBindings::class,
        AdvancedSettingsBindings::class,
        AboutBindings::class,
        AppUpdateBindings::class,
        DataSettingsBindings::class,
        OnboardingBindings::class,
        SyncBindings::class,
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
    val preferenceAppLocaleStore: PreferenceAppLocaleStore
    val workCloseGuard: WorkCloseGuard
    val openWindow: OpenWindow
    val homeDestinations: HomeDestinations
    val desktopNotifications: DesktopNotifications
    val systemFileChooser: SystemFileChooser
    val persistedWorkResume: PersistedWorkResume
    val jcefRuntime: JcefRuntime
    val challengePage: ChallengePage
    val windowActivity: WindowActivity
    val windowExtensionDialogs: WindowExtensionDialogs
    val appUpdateController: AppUpdateController
    val windowAnimePlayer: WindowAnimePlayer
    val activeProfileLock: ActiveProfileLock
    val profileRelock: ProfileRelock
    val appPresence: AppPresence
    val signInPrompts: SignInPrompts
    val systemSignIn: SystemSignIn

    // The app passes the system's folders; developer tools running the host headless pass folders of their own.
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides directories: DesktopDirectories): DesktopGraph
    }
}
