package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * The settings objects of LNReader's `Setting.json`, each stored as the JSON text of one key, as far as converting
 * them needs: src/hooks/persisted/useSettings.ts and src/hooks/persisted/useNovel/types.ts (LNReader/lnreader v2.1.4,
 * a0c20e209c8b). A field is null where the stored object lacks it, which writes nothing (D081). Fields kept only to
 * list a setting as left behind are read for being there, not for their value.
 */

/** `APP_SETTINGS`. */
@Serializable
internal class LnReaderAppSettings(
    val disableHapticFeedback: Boolean? = null,
    val showHistoryTab: Boolean? = null,
    val showUpdatesTab: Boolean? = null,
    val showLabelsInNav: Boolean? = null,
    val useFabForContinueReading: Boolean? = null,
    val disableLoadingAnimations: Boolean? = null,
    val dateFormat: String? = null,
    val relativeTimestamps: Boolean? = null,
    val useLibraryFAB: Boolean? = null,
    val smartUpdateSkipCompleted: Boolean? = null,
    val smartUpdateSkipUnstarted: Boolean? = null,
    val smartUpdateSkipWithUnread: Boolean? = null,
    /** Removed in LNReader 2.1.0 for the three skip rules; older backups still hold it. */
    val onlyUpdateOngoingNovels: Boolean? = null,
    val automaticLibraryUpdateIntervalHours: Int? = null,
    val automaticBackupIntervalHours: Int? = null,
    val automaticBackupDirectoryUri: String? = null,
    val updateLibraryOnLaunch: Boolean? = null,
    val downloadNewChapters: Boolean? = null,
    val refreshNovelMetadata: Boolean? = null,
    val chapterDownloadCooldownMs: Double? = null,
    val hideBackdrop: Boolean? = null,
    /** A [LnReaderChapterOrder] key, or the SQL older versions stored. */
    val defaultChapterSort: String? = null,
    val timeTrackingEnabled: Boolean? = null,
    val inactivityTimeoutMs: Double? = null,
)

/** `BROWSE_SETTINGS`. */
@Serializable
internal class LnReaderBrowseSettings(
    val showMyAnimeList: Boolean? = null,
    val showAniList: Boolean? = null,
    val globalSearchConcurrency: Int? = null,
)

/** `LIBRARY_SETTINGS`. */
@Serializable
internal class LnReaderLibrarySettings(
    /** LNReader's own category id; unset, or its built-in Default's, means the built-in Default. */
    val defaultCategoryId: Long? = null,
    val promptForCategoryOnAdd: Boolean? = null,
    val globalUpdateExcludeCategoryIds: List<Long>? = null,
    val globalUpdateIncludeCategoryIds: List<Long>? = null,
    /** `<column> <ASC|DESC>`, the SQL of LibrarySortOrder. */
    val sortOrder: String? = null,
    /** The SQL condition of one LibraryFilter. */
    val filter: String? = null,
    val showDownloadBadges: Boolean? = null,
    val showUnreadBadges: Boolean? = null,
    val showContinueReadingButton: Boolean? = null,
    val showNumberOfNovels: Boolean? = null,
    /** DisplayModes: 0 compact, 1 comfortable, 2 cover only, 3 list. */
    val displayMode: Int? = null,
    val novelsPerRow: Int? = null,
)

/** `CHAPTER_GENERAL_SETTINGS`. */
@Serializable
internal class LnReaderChapterGeneralSettings(
    val keepScreenOn: Boolean? = null,
    val fullScreenMode: Boolean? = null,
    val pageReader: Boolean? = null,
    val swipeGestures: Boolean? = null,
    val showScrollPercentage: Boolean? = null,
    val useVolumeButtons: Boolean? = null,
    val volumeButtonsOffset: Double? = null,
    val showBatteryAndTime: Boolean? = null,
    val autoScroll: Boolean? = null,
    val autoScrollInterval: Double? = null,
    val autoScrollOffset: Double? = null,
    val verticalSeekbar: Boolean? = null,
    val removeExtraParagraphSpacing: Boolean? = null,
    val bionicReading: Boolean? = null,
    val tapToScroll: Boolean? = null,
    @SerialName("TTSEnable") val ttsEnabled: Boolean? = null,
)

/** `CHAPTER_READER_SETTINGS`. Colours are CSS colour strings. */
@Serializable
internal class LnReaderChapterReaderSettings(
    val theme: String? = null,
    val textColor: String? = null,
    /** In pixels. */
    val textSize: Double? = null,
    val textAlign: String? = null,
    /** Each side's, in pixels. */
    val padding: Double? = null,
    /** One of readerFonts' families; empty for the chapter's own. */
    val fontFamily: String? = null,
    val lineHeight: Double? = null,
    /** From before code snippets, which LNReader turns it into on its first start. */
    val customCSS: String? = null,
    val customJS: String? = null,
    val customThemes: List<LnReaderReaderColors>? = null,
    val tts: LnReaderTtsSettings? = null,
    val epubLocation: String? = null,
    val epubUseAppTheme: Boolean? = null,
    val epubUseCustomCSS: Boolean? = null,
    val epubUseCustomJS: Boolean? = null,
    val epubIncludeChapterNumber: Boolean? = null,
    /** Text to find and what to put in its place; text of the form `/pattern/flags` is a regular expression. */
    val replaceText: Map<String, String>? = null,
    /** Text to remove, read as [replaceText]'s keys are. */
    val removeText: List<String>? = null,
    val codeSnippetsCSS: List<LnReaderCodeSnippet>? = null,
    val codeSnippetsJS: List<LnReaderCodeSnippet>? = null,
)

@Serializable
internal class LnReaderReaderColors(val backgroundColor: String, val textColor: String)

@Serializable
internal class LnReaderTtsSettings(
    /** The device's speech engine, by the other app's own description of it. */
    val engine: JsonElement? = null,
    /** The device's voice, by the other app's own description of it. */
    val voice: JsonElement? = null,
    val rate: Float? = null,
    val pitch: Float? = null,
    val autoPageAdvance: Boolean? = null,
    val scrollToTop: Boolean? = null,
)

@Serializable
internal class LnReaderCodeSnippet(val code: String = "")

/** `NOVEL_SETTINGS_<pluginId>_<novelPath>`: how one novel's chapter list is filtered, sorted and named. */
@Serializable
internal class LnReaderNovelSettings(
    /** ChapterFilterKeys, each one condition the shown chapters meet. */
    val filter: List<String> = emptyList(),
    val showChapterTitles: Boolean? = null,
    /** A [LnReaderChapterOrder] key; unset follows the app's default chapter sort. */
    val sort: String? = null,
    val excludedScanlators: List<String> = emptyList(),
)
