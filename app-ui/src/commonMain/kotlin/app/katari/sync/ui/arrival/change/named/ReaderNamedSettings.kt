package app.katari.sync.ui.arrival.change.named

import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.entry.interactions.reader.settings.BookDocumentReaderSettings
import app.katari.entry.interactions.reader.settings.BookDocumentReaderTheme
import app.katari.entry.interactions.reader.settings.BookTextRule
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.platform.presentation.platformPresentation
import app.katari.profiles.core.ProfileStore
import app.katari.sync.ui.arrival.change.listChange
import app.katari.sync.ui.arrival.change.setChanges
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The readers' settings changed only in the reader, read through the definitions the readers this platform has give
 * Settings: the manga reader's colour filter and long press, the book reader's saved colours, text rules and publisher
 * styles.
 */
internal fun readerNamedSettings(
    viewerSettings: EntryViewerSettingsFeature,
    profileStore: ProfileStore,
): List<NamedSetting> {
    val surfaces = viewerSettings.destinations.map { it.settings }
    return surfaces.filterIsInstance<MangaReaderSettings>().firstOrNull()?.let {
        mangaReader(it, profileStore)
    }.orEmpty() +
        surfaces.filterIsInstance<BookDocumentReaderSettings>().firstOrNull()?.let {
            bookReader(it, profileStore)
        }.orEmpty()
}

private fun mangaReader(manga: MangaReaderSettings, profileStore: ProfileStore): List<NamedSetting> {
    val colorFilter = manga.colorFilter
    return listOfNotNull(
        switchSetting(
            MR.strings.pref_custom_brightness,
            profileStore.booleanPreference(colorFilter.customBrightness.profilePreference),
        ),
        valueSetting(
            MR.strings.pref_custom_brightness,
            profileStore.intPreference(colorFilter.customBrightnessValue.profilePreference),
        ) { it.toString() },
        switchSetting(
            MR.strings.pref_custom_color_filter,
            profileStore.booleanPreference(colorFilter.colorFilter.profilePreference),
        ),
        valueSetting(
            MR.strings.sync_setting_color_filter_color,
            profileStore.intPreference(colorFilter.colorFilterValue.profilePreference),
        ) { argb -> "#" + argb.toUInt().toString(HEX).padStart(ARGB_DIGITS, '0').uppercase() },
        valueSetting(
            MR.strings.pref_color_filter_mode,
            profileStore.intPreference(colorFilter.colorFilterMode.profilePreference),
        ) { mode ->
            stringResource(MangaReaderSettings.ColorFilterMode.getOrNull(mode)?.first ?: MR.strings.label_default)
        },
        switchSetting(
            MR.strings.pref_grayscale,
            profileStore.booleanPreference(colorFilter.grayscale.profilePreference),
        ),
        switchSetting(
            MR.strings.pref_inverted_colors,
            profileStore.booleanPreference(colorFilter.invertedColors.profilePreference),
        ),
        // A platform that can't long press offers no such setting, so it has no name for it either
        platformPresentation.pageActionsOnLongPressLabel?.let { label ->
            switchSetting(label, profileStore.booleanPreference(manga.reading.readWithLongTap.profilePreference))
        },
    )
}

private fun bookReader(book: BookDocumentReaderSettings, profileStore: ProfileStore): List<NamedSetting> = listOf(
    namedSetting(
        MR.strings.sync_setting_saved_colors,
        profileStore.objectPreference(
            book.savedColors,
            BookDocumentReaderTheme::encodeCustomList,
            BookDocumentReaderTheme::decodeCustomList,
        ),
    ) { name, value, _ ->
        // Saved colours are named by their place in the list, as the reader's theme choices name them
        setChanges(name, value.before.toSet(), value.after.toSet(), { colors ->
            val place = value.after.indexOf(colors).takeIf { it >= 0 } ?: value.before.indexOf(colors)
            stringResource(MR.strings.book_document_reader_theme_custom, place + 1)
        })
    },
    namedSetting(
        MR.strings.pref_book_text_rules,
        profileStore.objectPreference(book.textRules, BookTextRule::encodeList, BookTextRule::decodeList),
    ) { name, value, _ ->
        val before = value.before.associateBy { it.id }
        val after = value.after.associateBy { it.id }
        listOfNotNull(
            listChange(name, MR.strings.sync_arrived_added, (after.keys - before.keys).map { after.getValue(it).find }),
            listChange(
                name,
                MR.strings.sync_arrived_removed,
                (before.keys - after.keys).map {
                    before.getValue(it).find
                },
            ),
            listChange(
                name,
                MR.strings.sync_arrived_edited,
                after.keys.filter { it in before && after[it] != before[it] }.map { after.getValue(it).find },
            ),
        )
    },
    switchSetting(
        MR.strings.book_document_reader_publisher_styles,
        profileStore.booleanPreference(book.publisherStylesSetting.profilePreference),
    ),
)

private const val HEX = 16
private const val ARGB_DIGITS = 8
