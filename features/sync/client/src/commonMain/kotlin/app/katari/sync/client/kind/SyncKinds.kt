package app.katari.sync.client.kind

import app.katari.sync.client.kind.activity.ActivityCompletionKind
import app.katari.sync.client.kind.activity.ActivitySegmentKind
import app.katari.sync.client.kind.activity.ActivitySessionKind
import app.katari.sync.client.kind.category.CategoryKind
import app.katari.sync.client.kind.category.CategoryMembershipKind
import app.katari.sync.client.kind.category.CategoryUpdateRulesKind
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entrychoice.DownloadPreferencesKind
import app.katari.sync.client.kind.entrychoice.EntryUpdateModeKind
import app.katari.sync.client.kind.entrychoice.ExcludedScanlatorKind
import app.katari.sync.client.kind.entrychoice.IntroTimesPickKind
import app.katari.sync.client.kind.entrychoice.PlaybackPreferencesKind
import app.katari.sync.client.kind.entrychoice.TheIntroDbPickKind
import app.katari.sync.client.kind.entrychoice.TranslationLanguagesKind
import app.katari.sync.client.kind.entrychoice.ViewerSettingKind
import app.katari.sync.client.kind.extension.ExtensionRepositoryKind
import app.katari.sync.client.kind.font.ImportedFontKind
import app.katari.sync.client.kind.history.HistoryKind
import app.katari.sync.client.kind.libraryupdate.LibraryUpdateKind
import app.katari.sync.client.kind.merge.MergeMemberKind
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.kind.question.MigrationQuestionKind
import app.katari.sync.client.kind.question.NotesQuestionKind
import app.katari.sync.client.kind.question.ProfileDeletionQuestionKind
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.kind.setting.ChosenProfileSettingKind
import app.katari.sync.client.kind.setting.ChosenSharedSettingKind
import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.kind.statistics.StatisticsEpochKind
import app.katari.sync.client.record.SyncedKind

/** Every kind of record this app version syncs. */
object SyncKinds {
    val all: List<SyncedKind<*>> = listOf(
        ProfileKind,
        EntryKind,
        ChapterKind,
        ProgressKind,
        HistoryKind,
        ActivitySessionKind,
        ActivitySegmentKind,
        ActivityCompletionKind,
        StatisticsEpochKind,
        LibraryUpdateKind,
        CategoryKind,
        CategoryMembershipKind,
        CategoryUpdateRulesKind,
        EntryUpdateModeKind,
        ExcludedScanlatorKind,
        ViewerSettingKind,
        DownloadPreferencesKind,
        TranslationLanguagesKind,
        IntroTimesPickKind,
        TheIntroDbPickKind,
        PlaybackPreferencesKind,
        MergeMemberKind,
        EntryReplacementKind,
        ExtensionRepositoryKind,
        ImportedFontKind,
        ProfileSettingKind,
        SharedSettingKind,
        ChosenProfileSettingKind,
        ChosenSharedSettingKind,
        SourceSettingKind,
        MigrationQuestionKind,
        NotesQuestionKind,
        ProfileDeletionQuestionKind,
    )

    private val byName = all.associateBy { it.name }

    init {
        require(byName.size == all.size) { "Kind names must be unique" }
    }

    fun named(name: String): SyncedKind<*>? = byName[name]
}
