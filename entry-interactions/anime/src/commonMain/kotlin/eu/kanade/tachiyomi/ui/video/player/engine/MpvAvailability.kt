package eu.kanade.tachiyomi.ui.video.player.engine

/** Whether the player can use mpv now, or what it would take. */
internal enum class MpvAvailability {
    Available,
    NotInstalled,

    /** Installed, but this Katari needs another release of it. */
    NeedsUpdate,

    /** Installed in a way no download fixes, such as signed with another key, or its libraries failed to load. */
    Unusable,

    /**
     * The platform has no mpv add-on, as on desktop, where mpv is the built-in engine: there is no other engine to
     * switch to, install or offer, and the title's engine choice doesn't apply.
     */
    Absent,
    ;

    val installable: Boolean
        get() = this == NotInstalled || this == NeedsUpdate
}
