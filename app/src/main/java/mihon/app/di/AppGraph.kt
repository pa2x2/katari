package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import eu.kanade.core.util.SourceLoadGraph
import eu.kanade.domain.track.service.DelayedTrackingGraph
import eu.kanade.presentation.browse.BrowsePresentationGraph
import eu.kanade.presentation.components.ComponentsPresentationGraph
import eu.kanade.presentation.entry.EntryPresentationGraph
import eu.kanade.presentation.library.update.LibraryUpdatePresentationGraph
import eu.kanade.presentation.more.MorePresentationGraph
import eu.kanade.presentation.more.onboarding.OnboardingPresentationGraph
import eu.kanade.presentation.more.settings.SettingsPresentationGraph
import eu.kanade.tachiyomi.AppStartupGraph
import eu.kanade.tachiyomi.data.backup.BackupGraph
import eu.kanade.tachiyomi.data.coil.EntryCoverGraph
import eu.kanade.tachiyomi.data.library.LibraryUpdateGraph
import eu.kanade.tachiyomi.data.notification.NotificationReceiverGraph
import eu.kanade.tachiyomi.data.statistics.StatisticsRecapGraph
import eu.kanade.tachiyomi.data.track.oauth.TrackLoginGraph
import eu.kanade.tachiyomi.extension.ExtensionGraph
import eu.kanade.tachiyomi.ui.base.delegate.ActivityDelegateGraph
import eu.kanade.tachiyomi.ui.base.migration.AppMigratingGraph
import eu.kanade.tachiyomi.ui.browse.BrowseGraph
import eu.kanade.tachiyomi.ui.entry.EntryScreenGraph
import eu.kanade.tachiyomi.ui.home.HomeGraph
import eu.kanade.tachiyomi.ui.library.LibraryGraph
import eu.kanade.tachiyomi.ui.main.MainGraph
import eu.kanade.tachiyomi.ui.more.OnboardingGraph
import eu.kanade.tachiyomi.ui.security.SecurityGraph
import eu.kanade.tachiyomi.ui.setting.SettingsGraph
import eu.kanade.tachiyomi.ui.webview.WebViewGraph
import eu.kanade.tachiyomi.util.CrashLogGraph
import eu.kanade.tachiyomi.util.system.TabletUiGraph
import eu.kanade.tachiyomi.widget.WidgetGraph
import mihon.app.di.injekt.AppInjektExports
import mihon.app.di.injekt.DomainInjektExports
import mihon.core.migration.MigrationGraph
import mihon.entry.interactions.host.AppEntryHostBindings
import mihon.feature.appupdate.ui.AppUpdateGraph
import mihon.feature.migration.work.SourceMigrationWorkGraph
import mihon.feature.profiles.ui.ProfilesGraph
import mihon.feature.runtime.FeatureRuntimeGraph

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        AppBindings::class,
        PreferenceBindings::class,
        DataBindings::class,
        AppEntryHostBindings::class,
        AppInjektExports::class,
        DomainInjektExports::class,
    ],
)
interface AppGraph :
    FeatureRuntimeGraph,
    ViewModelGraph,
    AppStartupGraph,
    ActivityDelegateGraph,
    AppMigratingGraph,
    AppUpdateGraph,
    BackupGraph,
    BrowseGraph,
    BrowsePresentationGraph,
    ComponentsPresentationGraph,
    CrashLogGraph,
    DelayedTrackingGraph,
    EntryCoverGraph,
    EntryPresentationGraph,
    EntryScreenGraph,
    ExtensionGraph,
    HomeGraph,
    LibraryGraph,
    LibraryUpdateGraph,
    LibraryUpdatePresentationGraph,
    MainGraph,
    MigrationGraph,
    MorePresentationGraph,
    NotificationReceiverGraph,
    OnboardingGraph,
    OnboardingPresentationGraph,
    ProfilesGraph,
    SecurityGraph,
    SettingsGraph,
    SettingsPresentationGraph,
    SourceLoadGraph,
    SourceMigrationWorkGraph,
    StatisticsRecapGraph,
    TabletUiGraph,
    TrackLoginGraph,
    WebViewGraph,
    WidgetGraph {
    val viewModelFactory: MetroViewModelFactory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
