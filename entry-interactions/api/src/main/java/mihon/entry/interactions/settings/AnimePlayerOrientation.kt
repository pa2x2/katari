package mihon.entry.interactions.settings

enum class AnimePlayerOrientation {
    /** Landscape for wide videos and portrait for tall ones, decided once the first frame's size is known. */
    FOLLOW_VIDEO,
    LANDSCAPE,
    PORTRAIT,

    /** Leaves the orientation to the system auto-rotate toggle. */
    SYSTEM,
}
