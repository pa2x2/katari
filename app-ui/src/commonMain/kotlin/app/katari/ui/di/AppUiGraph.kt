package app.katari.ui.di

import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.core.platform.share.ImageSharing
import app.katari.core.platform.share.LinkSharing
import app.katari.core.platform.share.TextSharing
import app.katari.presentation.kit.theme.PlatformKatariThemeGraph
import app.katari.profiles.ui.ProfilesGraph
import app.katari.sync.ui.SyncUiGraph
import app.katari.ui.buildinfo.AppBuild
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import eu.kanade.presentation.browse.BrowsePresentationGraph
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.presentation.entry.EntryPresentationGraph
import eu.kanade.presentation.library.update.LibraryUpdatePresentationGraph
import eu.kanade.presentation.more.onboarding.OnboardingGraph
import eu.kanade.presentation.more.settings.PreferenceScreenGraph
import eu.kanade.presentation.more.settings.screen.BookReaderSettingsGraph
import eu.kanade.presentation.more.settings.screen.DownloadSettingsGraph
import eu.kanade.presentation.more.settings.screen.LibrarySettingsGraph
import eu.kanade.presentation.more.settings.screen.about.AboutGraph
import eu.kanade.presentation.more.settings.screen.advanced.AdvancedSettingsGraph
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerSettingsGraph
import eu.kanade.presentation.more.settings.screen.appearance.AppearanceSettingsGraph
import eu.kanade.presentation.more.settings.screen.browse.BrowseSettingsGraph
import eu.kanade.presentation.more.settings.screen.data.DataSettingsGraph
import eu.kanade.presentation.more.settings.screen.debug.DebugInfoGraph
import eu.kanade.presentation.more.settings.screen.notifications.NotificationSettingsGraph
import eu.kanade.presentation.more.settings.screen.security.SecuritySettingsGraph
import eu.kanade.presentation.more.settings.screen.textrecognition.TextRecognitionSettingsGraph
import eu.kanade.presentation.more.settings.screen.touchpad.TouchpadSettingsGraph
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsGraph
import eu.kanade.presentation.more.settings.screen.tts.TtsSettingsGraph
import eu.kanade.presentation.more.settings.screen.viewer.ViewerSettingsGraph
import eu.kanade.presentation.util.SystemClockFormat
import eu.kanade.tachiyomi.ui.base.migration.AppMigratingGraph
import eu.kanade.tachiyomi.ui.browse.BrowseGraph
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesGraph
import eu.kanade.tachiyomi.ui.entry.EntryScreenGraph
import eu.kanade.tachiyomi.ui.home.HomeGraph
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationGraph
import eu.kanade.tachiyomi.ui.library.LibraryGraph
import eu.kanade.tachiyomi.ui.main.MainHostGraph
import eu.kanade.tachiyomi.ui.webview.WebViewGraph

/**
 * What the app's shared screens reach from the graph of the platform they run on. Each platform's application graph
 * extends it and provides it to composition through [LocalAppUiGraph].
 */
interface AppUiGraph :
    ViewModelGraph,
    PlatformKatariThemeGraph,
    ProfilesGraph,
    PreferenceScreenGraph,
    AppearanceSettingsGraph,
    LibrarySettingsGraph,
    DownloadSettingsGraph,
    BrowseSettingsGraph,
    ViewerSettingsGraph,
    BookReaderSettingsGraph,
    PlayerSettingsGraph,
    TranslationSettingsGraph,
    TtsSettingsGraph,
    TextRecognitionSettingsGraph,
    DataSettingsGraph,
    SecuritySettingsGraph,
    NotificationSettingsGraph,
    TouchpadSettingsGraph,
    AdvancedSettingsGraph,
    DebugInfoGraph,
    AboutGraph,
    OnboardingGraph,
    HomeGraph,
    MainHostGraph,
    AppMigratingGraph,
    HomeNavigationGraph,
    LibraryGraph,
    LibraryUpdatePresentationGraph,
    EntryPresentationGraph,
    EntryScreenGraph,
    BrowseGraph,
    BrowsePresentationGraph,
    SourcePreferencesGraph,
    WebViewGraph,
    SyncUiGraph {
    val viewModelFactory: MetroViewModelFactory
    val linkOpener: LinkOpener
    val textClipboard: TextClipboard
    val imageSharing: ImageSharing
    val linkSharing: LinkSharing
    val textSharing: TextSharing
    val appBuild: AppBuild
    val sourceRows: SourceRows
    val systemClockFormat: SystemClockFormat
}
