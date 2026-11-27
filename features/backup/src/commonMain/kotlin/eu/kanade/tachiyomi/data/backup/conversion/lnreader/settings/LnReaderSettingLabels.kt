package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings.Companion.AVAILABLE_PLUGINS
import tachiyomi.i18n.*

/**
 * LNReader's names for its settings Katari has nothing for and the places it keeps them, as the report lists them
 * (D091), and the keys the report leaves out: from LNReader's English strings and screens at LNReader/lnreader v2.1.4
 * a0c20e209c8b (workspace research/09). Places start where LNReader's settings, library, reader or a novel's screen
 * start; the reader's own sheet is Reader › General and Reader › TTS, beside the Settings' Reader tabs.
 */
internal object LnReaderSettingLabels {
    private val general = MR.strings.pref_category_general
    private val appearance = MR.strings.pref_category_appearance
    private val library = MR.strings.pref_category_library
    private val reader = MR.strings.pref_category_reader
    private val navigation = MR.strings.pref_reader_navigation
    private val browseSettings = listOf(MR.strings.browse, MR.strings.backup_conversion_lnreader_place_browse_settings)
    private val customCode = MR.strings.backup_conversion_lnreader_place_custom_code
    private val tts = MR.strings.backup_conversion_lnreader_place_tts
    private val exportEpub = MR.strings.backup_conversion_lnreader_place_export_epub

    val DISABLE_HAPTIC_FEEDBACK =
        lnReader(MR.strings.backup_conversion_lnreader_disable_haptic_feedback, general, general)
    val DISABLE_LOADING_ANIMATIONS =
        lnReader(MR.strings.backup_conversion_lnreader_disable_loading_animations, general, general)
    val CHAPTER_DOWNLOAD_COOLDOWN =
        lnReader(MR.strings.backup_conversion_lnreader_chapter_download_cooldown, general, general)
    val ENABLE_TIME_TRACKING = lnReader(
        MR.strings.backup_conversion_lnreader_enable_time_tracking,
        general,
        MR.strings.backup_conversion_lnreader_place_time_tracking,
    )
    val INACTIVITY_TIMEOUT = lnReader(
        MR.strings.backup_conversion_lnreader_inactivity_timeout,
        general,
        MR.strings.backup_conversion_lnreader_place_time_tracking,
    )
    val ALWAYS_SHOW_NAV_LABELS = lnReader(
        MR.strings.backup_conversion_lnreader_always_show_nav_labels,
        appearance,
        MR.strings.backup_conversion_lnreader_place_navbar,
    )
    val USE_FAB = lnReader(
        MR.strings.backup_conversion_lnreader_use_fab,
        appearance,
        MR.strings.backup_conversion_lnreader_place_novel_info,
    )
    val HIDE_BACKDROP = lnReader(
        MR.strings.backup_conversion_lnreader_hide_backdrop,
        appearance,
        MR.strings.backup_conversion_lnreader_place_novel_info,
    )
    val LIBRARY_RESUME_BUTTON = lnReader(MR.strings.backup_conversion_lnreader_library_resume_button, library, library)
    val UPDATE_LIBRARY_ON_LAUNCH =
        lnReader(MR.strings.backup_conversion_lnreader_update_library_on_launch, library, library)
    val DEFAULT_CATEGORY = lnReader(MR.strings.backup_conversion_lnreader_default_category, library, library)
    val AUTOMATIC_BACKUP_LOCATION = lnReader(
        MR.strings.backup_conversion_lnreader_automatic_backup_location,
        MR.strings.backup_conversion_lnreader_place_backup,
        MR.strings.backup_conversion_lnreader_place_local_backup,
    )

    val SHOW_ANILIST = lnReader(
        MR.strings.backup_conversion_lnreader_show_anilist,
        *browseSettings.toTypedArray(),
        MR.strings.backup_conversion_lnreader_place_discover,
    )
    val SHOW_MY_ANIME_LIST = lnReader(
        MR.strings.backup_conversion_lnreader_show_my_anime_list,
        *browseSettings.toTypedArray(),
        MR.strings.backup_conversion_lnreader_place_discover,
    )
    val CONCURRENT_SEARCHES = lnReader(
        MR.strings.backup_conversion_lnreader_concurrent_searches,
        *browseSettings.toTypedArray(),
        MR.strings.backup_conversion_lnreader_place_global_search,
    )
    val LANGUAGES = lnReader(MR.strings.backup_conversion_lnreader_languages, *browseSettings.toTypedArray())
    val USER_AGENT = lnReader(
        MR.strings.backup_conversion_lnreader_user_agent,
        MR.strings.pref_category_advanced,
        MR.strings.label_network,
    )

    val SWIPE_BETWEEN_CHAPTERS =
        lnReader(MR.strings.backup_conversion_lnreader_swipe_between_chapters, reader, navigation)
    val VOLUME_BUTTON_OFFSET = lnReader(MR.strings.backup_conversion_lnreader_volume_button_offset, reader, navigation)
    val AUTO_SCROLL = lnReader(MR.strings.backup_conversion_lnreader_auto_scroll, reader, navigation)
    val AUTO_SCROLL_INTERVAL = lnReader(MR.strings.backup_conversion_lnreader_auto_scroll_interval, reader, navigation)
    val AUTO_SCROLL_OFFSET = lnReader(MR.strings.backup_conversion_lnreader_auto_scroll_offset, reader, navigation)
    val VERTICAL_SEEKBAR = lnReader(MR.strings.backup_conversion_lnreader_vertical_seekbar, reader, navigation)
    val TAP_TO_SCROLL = lnReader(MR.strings.backup_conversion_lnreader_tap_to_scroll, reader, navigation)
    val BIONIC_READING = lnReader(
        MR.strings.backup_conversion_lnreader_bionic_reading,
        reader,
        MR.strings.backup_conversion_lnreader_place_accessibility,
    )
    val FONT_STYLE =
        lnReader(MR.strings.backup_conversion_lnreader_font_style, reader, MR.strings.pref_category_display)
    val REMOVE_EXTRA_SPACING = lnReader(MR.strings.backup_conversion_lnreader_remove_extra_spacing, reader, general)

    val ENABLE_TTS = lnReader(MR.strings.backup_conversion_lnreader_enable_tts, reader, tts)
    val TTS_ENGINE = lnReader(MR.strings.backup_conversion_lnreader_tts_engine, reader, tts)
    val TTS_VOICE = lnReader(MR.strings.backup_conversion_lnreader_tts_voice, reader, tts)
    val TTS_SPEED = lnReader(MR.strings.backup_conversion_lnreader_tts_speed, reader, tts)
    val TTS_PITCH = lnReader(MR.strings.backup_conversion_lnreader_tts_pitch, reader, tts)
    val TTS_AUTO_PAGE_ADVANCE = lnReader(MR.strings.backup_conversion_lnreader_tts_auto_page_advance, reader, tts)
    val TTS_SCROLL_TO_TOP = lnReader(MR.strings.backup_conversion_lnreader_tts_scroll_to_top, reader, tts)

    val CSS_SNIPPETS = lnReader(MR.strings.backup_conversion_lnreader_css_snippets, customCode)
    val JS_SNIPPETS = lnReader(MR.strings.backup_conversion_lnreader_js_snippets, customCode)
    val TEXT_RULES = lnReader(MR.strings.backup_conversion_lnreader_text_rules, customCode)

    val EPUB_DIRECTORY = lnReader(MR.strings.backup_conversion_lnreader_epub_directory, exportEpub)
    val EPUB_APPLY_READER_THEME = lnReader(MR.strings.backup_conversion_lnreader_epub_apply_reader_theme, exportEpub)
    val EPUB_INCLUDE_CSS = lnReader(MR.strings.backup_conversion_lnreader_epub_include_css, exportEpub)
    val EPUB_INCLUDE_JS = lnReader(MR.strings.backup_conversion_lnreader_epub_include_js, exportEpub)
    val EPUB_INCLUDE_CHAPTER_NUMBER =
        lnReader(MR.strings.backup_conversion_lnreader_epub_include_chapter_number, exportEpub)

    /** Settings LNReader keeps under keys of their own, by key. */
    val NO_COUNTERPART: Map<String, OtherAppSettingLabel> = mapOf(
        "CUSTOM_ACCENT_COLOR" to lnReader(
            MR.strings.backup_conversion_lnreader_accent_color,
            appearance,
            MR.strings.pref_app_theme,
        ),
        "APP_LOCALE" to lnReader(
            MR.strings.backup_conversion_lnreader_app_language,
            appearance,
            MR.strings.pref_category_display,
        ),
        "SHOW_LAST_UPDATE_TIME" to lnReader(
            MR.strings.backup_conversion_lnreader_show_last_update_time,
            library,
            MR.strings.backup_conversion_lnreader_place_global_update,
        ),
        "GENRE_TAXONOMY" to lnReader(MR.strings.backup_conversion_lnreader_genre_taxonomy),
        "snippetEditorHighlightMode" to lnReader(MR.strings.backup_conversion_lnreader_syntax_highlighting, customCode),
    )

    /**
     * Kept by the app rather than chosen by a person: caches, one-time flags, timestamps and the skipped app update;
     * numbers, which LNReader's backup never holds; tracker sign-ins (D087); and keys LNReader leaves out of backups,
     * which older versions may still hold.
     */
    val IGNORED = setOf(
        AVAILABLE_PLUGINS,
        "LAST_USED_PLUGIN",
        "IS_ONBOARDED",
        "TRACKER_MIGRATION_V1_COMPLETED",
        "IGNORED_UPDATE_VERSION",
        "LAST_UPDATE_CHECK",
        "APP_THEME_ID",
        "TRACKERS",
        "ACTIVE_TRACKER",
        "TRACKED_NOVEL_PREFIX",
        "INSTALL_PLUGINS",
        "APP_SERVICE",
        "SELF_HOST_BACKUP",
        "LAST_UPDATE_TIME",
    )

    /**
     * Keys of one novel each: the last chapter read, which Katari works out from the chapters (D086), the reader's page,
     * a number, and a migration flag.
     */
    val IGNORED_PREFIXES = listOf("LAST_READ_PREFIX_", "NOVEL_PAGE_INDEX_PREFIX_", "TRACKED_NOVEL_MIGRATION_V1_")

    private fun lnReader(label: StringResource, vararg place: StringResource) = OtherAppSettingLabel(
        label,
        place.toList(),
    )
}
