package mihon.app.ui.di

import app.katari.sync.ui.SyncUiGraph
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
import eu.kanade.presentation.more.settings.screen.security.SecuritySettingsGraph
import eu.kanade.presentation.more.settings.screen.textrecognition.TextRecognitionSettingsGraph
import eu.kanade.presentation.more.settings.screen.tracking.TrackingSettingsGraph
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
import mihon.app.ui.buildinfo.AppBuild
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.link.LinkOpener
import mihon.core.platform.share.ImageSharing
import mihon.core.platform.share.LinkSharing
import mihon.core.platform.share.TextSharing
import mihon.feature.profiles.ui.ProfilesGraph
import mihon.presentation.kit.theme.PlatformKatariThemeGraph

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
    TrackingSettingsGraph,
    ViewerSettingsGraph,
    BookReaderSettingsGraph,
    PlayerSettingsGraph,
    TranslationSettingsGraph,
    TtsSettingsGraph,
    TextRecognitionSettingsGraph,
    DataSettingsGraph,
    SecuritySettingsGraph,
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
