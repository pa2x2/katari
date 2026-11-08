package app.katari.sync.client.setting.scope

/**
 * The book reader: `BookDocumentReaderPreferences`, its remembered processors and remote-resource consents. Text,
 * theme and paging sync; page layout, text size, margins, bars and keys stay with the screen they suit.
 */
internal val BookReaderScopes = PreferenceScopeTable {
    person(
        "book_document_reader_reading_mode",
        "book_document_reader_tap_zones",
        "book_document_reader_tap_inversion",
        "book_document_reader_animate_pages",
        "book_document_reader_theme_mode",
        "book_document_reader_saved_colors",
        "book_document_reader_text_rules",
        "book_document_reader_publisher_styles",
        "book_document_reader_font",
        "book_document_reader_line_spacing",
        "book_document_reader_text_alignment",
        "book_document_reader_show_text_selection_menu",
        "book_document_reader_show_reading_progress",
        "book_document_reader_reading_progress_style",
        "book_document_reader_chapter_transition",
    )
    device(
        "book_document_reader_page_layout",
        "book_document_reader_text_size_percent",
        "book_document_reader_margins",
        "book_document_reader_volume_keys",
        "book_document_reader_invert_volume_keys",
        "book_document_reader_keep_screen_alive",
        "book_document_reader_show_status_bar",
        "book_document_reader_show_time_and_battery",
        "book_document_reader_show_navigation_bar",
        "book_document_reader_input_guide_shown",
    )

    // Which processor opens each kind of publication depends on the processors this device has; consents are keyed
    // by a hash of the publication's ID, which can be a path on this device.
    devicePrefix("book_processor_", "entry_interactions_book_remote_resource_consent_")
}
