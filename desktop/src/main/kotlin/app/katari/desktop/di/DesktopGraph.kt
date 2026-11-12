package app.katari.desktop.di

import app.katari.appupdate.AppUpdateController
import app.katari.core.common.web.ChallengePage
import app.katari.data.di.DatabaseBindings
import app.katari.data.di.RepositoryBindings
import app.katari.desktop.appearance.PreferenceAppLocaleStore
import app.katari.desktop.application.AppRestart
import app.katari.desktop.application.classdata.ClassDataArchive
import app.katari.desktop.application.installation.AppInstallation
import app.katari.desktop.autostart.LoginStart
import app.katari.desktop.display.backend.DisplayBackend
import app.katari.desktop.display.size.InterfaceSizePreferences
import app.katari.desktop.display.size.SessionScale
import app.katari.desktop.extension.ExtensionHostGraph
import app.katari.desktop.extension.dialog.WindowExtensionDialogs
import app.katari.desktop.home.HomeDestinations
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.player.WindowAnimePlayer
import app.katari.desktop.presence.AppPresence
import app.katari.desktop.profile.ActiveProfileLock
import app.katari.desktop.profile.ProfileRelock
import app.katari.desktop.security.signin.SignInPrompts
import app.katari.desktop.security.signin.SystemSignIn
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.tray.AppTray
import app.katari.desktop.webview.ChromiumUpkeep
import app.katari.desktop.window.MainWindowVisibility
import app.katari.desktop.window.OpenWindow
import app.katari.desktop.window.WindowPreferences
import app.katari.desktop.work.PersistedWorkResume
import app.katari.desktop.work.WorkCloseGuard
import app.katari.entry.interactions.host.AppEntryHostBindings
import app.katari.extension.runtime.jvm.application.WindowActivity
import app.katari.presentation.kit.files.SystemFileChooser
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.AppUiScope
import app.katari.ui.host.AppProcessGraph
import app.katari.webview.jcef.runtime.JcefRuntime
import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

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
    val sessionScale: SessionScale
    val interfaceSizePreferences: InterfaceSizePreferences
    val appRestart: AppRestart
    val classDataArchive: ClassDataArchive
    val appInstallation: AppInstallation
    val preferenceAppLocaleStore: PreferenceAppLocaleStore
    val workCloseGuard: WorkCloseGuard
    val openWindow: OpenWindow
    val mainWindowVisibility: MainWindowVisibility
    val appTray: AppTray
    val loginStart: LoginStart
    val homeDestinations: HomeDestinations
    val desktopNotifications: DesktopNotifications
    val systemFileChooser: SystemFileChooser
    val persistedWorkResume: PersistedWorkResume
    val jcefRuntime: JcefRuntime
    val chromiumUpkeep: ChromiumUpkeep
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
