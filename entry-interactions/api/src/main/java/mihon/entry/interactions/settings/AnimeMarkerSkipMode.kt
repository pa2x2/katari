package mihon.entry.interactions.settings

/** What the player does when it reaches an intro, recap, credits or preview the source marked. */
enum class AnimeMarkerSkipMode {
    BUTTON,

    /** The button counts down and then skips, unless stopped; each marker counts down once per visit, as AUTOMATIC. */
    COUNTDOWN,

    /** Skips each marker once per visit; an intro that carries story still only gets the button. */
    AUTOMATIC,
    OFF,
}
