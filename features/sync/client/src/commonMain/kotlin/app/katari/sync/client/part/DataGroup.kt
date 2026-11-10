package app.katari.sync.client.part

/**
 * The data a device can choose not to sync, each a set of record kinds. Library and reading progress have none: every
 * other record belongs to a title, so they always sync.
 */
enum class DataGroup {
    HISTORY,
    STATISTICS,

    /** The choices made for one title: its reader, player, download, translation, scanlator and intro choices. */
    TITLE_CHOICES,
    EXTENSION_REPOSITORIES,

    /** Which extensions each device installed and uninstalled, which the others are offered to do too. */
    INSTALLED_EXTENSIONS,
    FONTS,
    COVERS,

    /** Categories, what is in them, and the settings that name them. */
    CATEGORIES,
}
