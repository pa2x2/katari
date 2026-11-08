package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import eu.kanade.core.util.SourceLoadGraph
import eu.kanade.presentation.components.ComponentsPresentationGraph
import eu.kanade.tachiyomi.AppStartupGraph
import eu.kanade.tachiyomi.data.backup.BackupGraph
import eu.kanade.tachiyomi.data.chapterlist.ChapterListWalkGraph
import eu.kanade.tachiyomi.data.coil.ImageLoaderGraph
import eu.kanade.tachiyomi.data.library.LibraryUpdateGraph
import eu.kanade.tachiyomi.data.notification.NotificationReceiverGraph
import eu.kanade.tachiyomi.data.schedule.ClockChangeScheduleGraph
import eu.kanade.tachiyomi.data.statistics.StatisticsRecapGraph
import eu.kanade.tachiyomi.extension.ExtensionGraph
import eu.kanade.tachiyomi.ui.base.delegate.ActivityDelegateGraph
import eu.kanade.tachiyomi.ui.main.MainGraph
import eu.kanade.tachiyomi.ui.security.SecurityGraph
import eu.kanade.tachiyomi.util.system.TabletUiGraph
import eu.kanade.tachiyomi.widget.WidgetGraph
import mihon.app.di.injekt.ExtensionInjektGraph
import mihon.app.sync.SyncWorkGraph
import mihon.app.ui.di.AppUiGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings
import mihon.entry.interactions.host.AndroidEntryHostBindings
import mihon.entry.interactions.host.AppEntryHostBindings
import mihon.feature.appupdate.ui.AppUpdateGraph
import mihon.feature.migration.work.SourceMigrationWorkGraph

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        AppBindings::class,
        ImageBindings::class,
        PreferenceBindings::class,
        ProfileBindings::class,
        DatabaseBindings::class,
        RepositoryBindings::class,
        SourceBindings::class,
        AppEntryHostBindings::class,
        AndroidEntryHostBindings::class,
        WorkBindings::class,
        BackupBindings::class,
        TranslationSetupBindings::class,
        LinkBindings::class,
        ClipboardBindings::class,
        ShareBindings::class,
        DestinationBindings::class,
        WebViewBindings::class,
        EntryScreenBindings::class,
        ThemeBindings::class,
        DisplayBindings::class,
        LanguageBindings::class,
        ViewerSettingsBindings::class,
        TextRecognitionSettingsBindings::class,
        AuthenticationBindings::class,
        ExtensionBindings::class,
        AdvancedSettingsBindings::class,
        AboutBindings::class,
        DataSettingsBindings::class,
        OnboardingBindings::class,
        SyncBindings::class,
    ],
)
interface AppGraph :
    AppUiGraph,
    AppStartupGraph,
    ExtensionInjektGraph,
    ActivityDelegateGraph,
    AppUpdateGraph,
    BackupGraph,
    ChapterListWalkGraph,
    ClockChangeScheduleGraph,
    ComponentsPresentationGraph,
    ExtensionGraph,
    ImageLoaderGraph,
    LibraryUpdateGraph,
    MainGraph,
    NotificationReceiverGraph,
    SecurityGraph,
    SourceLoadGraph,
    SourceMigrationWorkGraph,
    StatisticsRecapGraph,
    SyncWorkGraph,
    TabletUiGraph,
    WidgetGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
