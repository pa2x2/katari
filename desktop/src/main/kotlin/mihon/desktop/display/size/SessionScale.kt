package mihon.desktop.display.size

/** The scale the desktop session sets for interface text and controls. */
interface SessionScale {
    /**
     * Pixels per dp the session asks for, where the window toolkit leaves the scale to the app; null where the
     * window's own density already is the session's.
     */
    val pixelsPerDp: Float?
}
