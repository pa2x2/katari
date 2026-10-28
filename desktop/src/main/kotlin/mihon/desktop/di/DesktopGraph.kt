package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings
import mihon.entry.interactions.catalogue.EntryCatalogueBindings
import mihon.entry.interactions.child.EntryChildBindings
import mihon.entry.interactions.download.EntryDownloadBindings
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindings
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveBindings
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalBindings
import mihon.entry.interactions.media.EntryMediaBindings
import mihon.entry.interactions.merge.EntryMergeBindings
import mihon.entry.interactions.migration.EntryMigrationBindings
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationBindings
import mihon.entry.interactions.reader.settings.ReaderSettingsBindings
import mihon.entry.interactions.settings.EntryInteractionPreferencesBindings
import mihon.entry.interactions.state.EntryStateBackupParticipantBindings
import mihon.entry.interactions.state.EntryStateMigrationParticipantBindings
import mihon.entry.interactions.tracking.EntryTrackingMergeParticipantBindings
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationParticipantBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    // Desktop has no startup work, merges or migrations yet, so it leaves out what shared features contribute to them,
    // and the features it doesn't host yet with what they contribute to backups. Reading progress needs the merge
    // feature, so its backup state is left out too. The download features also need the merge host and source access;
    // DownloadBindings stands in for them. Deleting entries needs the download and merge features; EntryRemovalBindings
    // stands in for it. Merges also decide how the library groups entries, which entries own chapters, which entry a
    // member opens as, what the library can merge and how entries join and leave it; MergeBindings stands in for all
    // of them, and MigrationBindings for the migration feature the library asks what it can migrate. Moving entries
    // between profiles needs the merge and download features too; ProfileBindings stands in for it. The catalogue
    // feature reaches sources through Android's extension loading; SourceBindings stands in for the source descriptions
    // the library reads from it. Previews need the child list feature; MediaBindings stands in for them. Without
    // readers, no reader shares a setting with the others; ViewerSettingsBindings stands in for the registry the reader
    // settings would build.
    excludes = [
        EntryTrackingMergeParticipantBindings::class,
        EntryStateMigrationParticipantBindings::class,
        EntryStateBackupParticipantBindings::class,
        EntryTranslationLanguagesMigrationParticipantBindings::class,
        EntryDownloadBindings::class,
        EntryChildBindings::class,
        EntryMediaBindings::class,
        EntryMergeBindings::class,
        EntryMigrationBindings::class,
        ReaderChapterPreparationBindings::class,
        ReaderSettingsBindings::class,
        EntryInteractionPreferencesBindings::class,
        EntryDestructiveRemovalBindings::class,
        EntryCatalogueBindings::class,
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
        ShareBindings::class,
        DestinationBindings::class,
        ThemeBindings::class,
        LanguageBindings::class,
        AuthenticationBindings::class,
        ExtensionBindings::class,
        MediaBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
        LibraryBindings::class,
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
interface DesktopGraph : AppUiGraph {
    val imageLoader: ImageLoader
}
