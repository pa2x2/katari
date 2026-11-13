package app.katari.sync.client.setting.scope

/**
 * Settings changed together from a menu outside Settings, such as the library menu's filters, which no Settings screen
 * lists one by one. They sync or stay on the device as one ([app.katari.sync.client.part.ChoiceSet.menusOff]), with
 * the area their keys belong to.
 */
enum class MenuSettings {
    /** The library's sort, from the library menu's Sort tab. */
    LIBRARY_SORT,

    /** The library's filters, from the library menu's Filter tab. */
    LIBRARY_FILTERS,

    /** How a title's chapter list is filtered, sorted and shown, saved by "Set as default" in its chapter menu. */
    CHAPTER_LIST_DEFAULTS,

    /** The Updates feed's filters, from its filter sheet. */
    UPDATES_FILTERS,

    /** The source languages shown and the sources pinned or hidden, from Browse's source and extension filters. */
    SOURCES,

    /** The feeds saved in Browse, with their presets, from its Feeds tab. */
    SAVED_FEEDS,
}
