package app.katari.desktop.display.scroll.natural

/**
 * What the display says of whether its touchpads scroll naturally, moving content the way the fingers go, which turns
 * the scrolling it reports against the fingers.
 */
internal sealed interface DisplayTouchpads {
    /** An X server that drives the touchpads itself; [natural] is null when it has none, or they differ. */
    data class Server(val natural: Boolean?) : DisplayTouchpads

    /** Xwayland, which gets the touchpads' scrolling from the compositor already turned, so only the compositor knows. */
    data object Compositor : DisplayTouchpads
}
