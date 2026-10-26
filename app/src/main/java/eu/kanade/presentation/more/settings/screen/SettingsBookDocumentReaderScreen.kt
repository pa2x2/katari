package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import mihon.app.di.appGraph
import mihon.entry.interactions.reader.settings.BookDocumentLineSpacing
import mihon.entry.interactions.reader.settings.BookDocumentMargins
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderProgressStyle
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.reader.settings.BookDocumentTextAlignment
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.presetNameRes
import mihon.entry.interactions.reader.settings.systemNameRes
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsBookDocumentReaderScreen : AppEntryViewerSettingsScreenProjection() {
    override val surfaceId = BookDocumentReaderSettings.SURFACE_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_book_document_reader

    @Composable
    override fun getSurfacePreferences(): List<Preference> {
        val context = LocalContext.current
        val provider = remember { context.appGraph.bookDocumentReaderSettings }
        val binder = remember { context.appGraph.viewerSettingBinder }
        val readingMode = remember(provider, binder) { binder.bind(provider.readingModeSetting).asProfilePreference() }
        val tapZones = remember(provider, binder) { binder.bind(provider.tapZonesSetting).asProfilePreference() }
        val tapInversion =
            remember(provider, binder) { binder.bind(provider.tapInversionSetting).asProfilePreference() }
        val animatePages =
            remember(provider, binder) { binder.bind(provider.animatePagesSetting).asProfilePreference() }
        val volumeKeys = remember(provider, binder) { binder.bind(provider.volumeKeysSetting).asProfilePreference() }
        val invertVolumeKeys =
            remember(provider, binder) { binder.bind(provider.invertVolumeKeysSetting).asProfilePreference() }
        val savedColors by provider.savedColors.collectAsState()
        val theme = remember(provider, binder) {
            binder.bind(provider.themeSetting).asProfilePreference()
        }
        val textSize = remember(provider, binder) {
            binder.bind(provider.textSizeSetting).asProfilePreference()
        }
        val fontLibrary = remember { context.appGraph.bookDocumentReaderFontLibrary }
        val importedFonts = remember(fontLibrary) {
            fontLibrary.importedFonts(context.appGraph.activeProfileProvider.activeProfileId)
        }
        val font = remember(provider, binder) {
            val binding = binder.bind(provider.fontSetting)
            // A restore doesn't bring imported font files back; such a choice falls back to Book.
            val chosen = binding.resolveProfile().profileValue
            if (chosen is BookDocumentReaderFont.Imported && chosen !in importedFonts) binding.resetProfileValue()
            binding.asProfilePreference()
        }
        val lineSpacing = remember(provider, binder) {
            binder.bind(provider.lineSpacingSetting).asProfilePreference()
        }
        val margins = remember(provider, binder) { binder.bind(provider.marginsSetting).asProfilePreference() }
        val textAlignment = remember(provider, binder) {
            binder.bind(provider.textAlignmentSetting).asProfilePreference()
        }
        val keepScreenAlive = remember(provider, binder) {
            binder.bind(provider.keepScreenAliveSetting).asProfilePreference()
        }
        val showStatusBar = remember(provider, binder) {
            binder.bind(provider.showStatusBarSetting).asProfilePreference()
        }
        val showNavigationBar = remember(provider, binder) {
            binder.bind(provider.showNavigationBarSetting).asProfilePreference()
        }
        val showTextSelectionMenu = remember(provider, binder) {
            binder.bind(provider.showTextSelectionMenuSetting).asProfilePreference()
        }
        val showReadingProgress = remember(provider, binder) {
            binder.bind(provider.showReadingProgressSetting).asProfilePreference()
        }
        val readingProgressStyle = remember(provider, binder) {
            binder.bind(provider.readingProgressStyleSetting).asProfilePreference()
        }
        val chapterTransitionMode = remember(provider, binder) {
            binder.bind(provider.chapterTransitionModeSetting).asProfilePreference()
        }
        val readingProgressVisible by showReadingProgress.collectAsState()
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_reading_mode),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = readingMode,
                        title = stringResource(MR.strings.pref_viewer_type),
                        entries = BookDocumentReadingMode.entries.associateWith { stringResource(it.stringRes) },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = tapZones,
                        title = stringResource(MR.strings.pref_viewer_nav),
                        entries = mihon.entry.viewer.chrome.ui.navigation.readerTapZoneLabels
                            .mapIndexed { index, label -> index to stringResource(label) }.toMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = tapInversion,
                        title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                        entries = mihon.entry.viewer.chrome.ui.navigation.readerTapInversionLabels
                            .mapIndexed { index, label -> index to stringResource(label) }.toMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        animatePages,
                        title = stringResource(MR.strings.pref_page_transitions),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        volumeKeys,
                        title = stringResource(MR.strings.pref_read_with_volume_keys),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        invertVolumeKeys,
                        title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                    ),
                ),
            ),

            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_appearance),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = theme,
                        entries = buildMap {
                            BookDocumentReaderTheme.presets.forEach {
                                put(it, stringResource(requireNotNull(it.presetNameRes)))
                            }
                            savedColors.forEachIndexed { index, colors ->
                                put(colors, stringResource(MR.strings.book_document_reader_theme_custom, index + 1))
                            }
                        },
                        title = stringResource(MR.strings.pref_book_document_reader_theme),
                    ),
                    Preference.PreferenceItem.StepperPreference(
                        preference = textSize,
                        valueRange = BookDocumentReaderSettings.TEXT_SIZE_RANGE,
                        step = BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT,
                        valueFormatter = { "$it%" },
                        inputSuffix = "%",
                        decreaseContentDescription = stringResource(MR.strings.action_decrease_text_size),
                        increaseContentDescription = stringResource(MR.strings.action_increase_text_size),
                        editContentDescription = stringResource(MR.strings.action_edit_text_size),
                        title = stringResource(MR.strings.pref_book_document_reader_text_size),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = font,
                        entries = (BookDocumentReaderFont.system + importedFonts).associateWith {
                            when (it) {
                                is BookDocumentReaderFont.Imported -> it.displayName
                                else -> stringResource(requireNotNull(it.systemNameRes))
                            }
                        },
                        title = stringResource(MR.strings.pref_book_document_reader_font),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = lineSpacing,
                        entries = BookDocumentLineSpacing.entries.associateWith { stringResource(it.stringRes) },
                        title = stringResource(MR.strings.pref_book_document_reader_line_spacing),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = margins,
                        entries = BookDocumentMargins.entries.associateWith { stringResource(it.stringRes) },
                        title = stringResource(MR.strings.pref_book_document_reader_margins),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = textAlignment,
                        entries = BookDocumentTextAlignment.entries.associateWith { stringResource(it.stringRes) },
                        title = stringResource(MR.strings.pref_book_document_reader_text_alignment),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_behavior),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = keepScreenAlive,
                        title = stringResource(MR.strings.pref_book_document_reader_keep_screen_alive),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = chapterTransitionMode,
                        entries = ChapterTransitionMode.entries.associateWith { stringResource(it.titleRes) },
                        title = stringResource(MR.strings.pref_chapter_transition),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showTextSelectionMenu,
                        title = stringResource(MR.strings.pref_book_document_reader_show_text_selection_menu),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showStatusBar,
                        title = stringResource(MR.strings.pref_reader_show_status_bar),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showNavigationBar,
                        title = stringResource(MR.strings.pref_reader_show_navigation_bar),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showReadingProgress,
                        title = stringResource(MR.strings.pref_book_document_reader_show_reading_progress),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = readingProgressStyle,
                        entries = mapOf(
                            BookDocumentReaderProgressStyle.EDGE_FILL_RAIL to
                                stringResource(MR.strings.book_document_reader_progress_edge_fill_rail),
                            BookDocumentReaderProgressStyle.EDGE_POSITION_MARKER to
                                stringResource(MR.strings.book_document_reader_progress_edge_position_marker),
                            BookDocumentReaderProgressStyle.BOTTOM_HAIRLINE to
                                stringResource(MR.strings.book_document_reader_progress_bottom_hairline),
                            BookDocumentReaderProgressStyle.PERCENTAGE to
                                stringResource(MR.strings.book_document_reader_progress_percentage),
                        ),
                        title = stringResource(MR.strings.pref_book_document_reader_reading_progress_style),
                        visible = readingProgressVisible,
                    ),
                ),
            ),
        )
    }
}
