package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings
import mihon.entry.interactions.child.EntryChildBindings
import mihon.entry.interactions.download.EntryDownloadBindings
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindings
import mihon.entry.interactions.media.EntryMediaBindings
import mihon.entry.interactions.merge.EntryMergeBindings
import mihon.entry.interactions.migration.EntryMigrationBindings
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationBindings
import mihon.entry.interactions.settings.EntryInteractionPreferencesBindings
import mihon.entry.interactions.state.EntryStateParticipantBindings
import mihon.entry.interactions.tracking.EntryTrackingParticipantBindings
import mihon.entry.interactions.translation.EntryTranslationLanguagesParticipantBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    // Desktop has no startup work, backups, merges or migrations yet, so it leaves out what shared features contribute
    // to them, and the features it doesn't host yet that contribute to them. The download features also need the merge
    // host and source access; DownloadBindings stands in for them.
    excludes = [
        EntryTrackingParticipantBindings::class,
        EntryStateParticipantBindings::class,
        EntryTranslationLanguagesParticipantBindings::class,
        EntryDownloadBindings::class,
        EntryChildBindings::class,
        EntryMediaBindings::class,
        EntryMergeBindings::class,
        EntryMigrationBindings::class,
        ReaderChapterPreparationBindings::class,
        EntryInteractionPreferencesBindings::class,
    ],
    bindingContainers = [
        ApplicationBindings::class,
        StorageBindings::class,
        PreferenceBindings::class,
        NetworkBindings::class,
        ImageBindings::class,
        LinkBindings::class,
        ClipboardBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
        LibraryUpdateBindings::class,
        DownloadBindings::class,
        TranslationBindings::class,
        TrackerBindings::class,
        EntryTrackingHostBindings::class,
    ],
)
interface DesktopGraph : AppUiGraph {
    val imageLoader: ImageLoader
}
