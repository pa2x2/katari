package mihon.entry.interactions.player

/**
 * The desktop's own media controls for the player, outside its windows: the shell's media widget and the media keys,
 * which MPRIS feeds on Linux. The player [show]s itself there for as long as it is open, paused included, and
 * [AnimePlayerMediaSession.close]s when it closes; feed clips never do.
 */
interface AnimePlayerMediaControls {
    /**
     * Puts the player in the desktop's controls, which send what the viewer asks for to [commands]. Returns at once;
     * where the desktop has no such controls the session shows nothing.
     */
    fun show(commands: AnimePlayerMediaCommands): AnimePlayerMediaSession
}

interface AnimePlayerMediaSession : AutoCloseable {
    /** Called on the UI thread whenever what plays changes, the position included, so it must be cheap. */
    fun update(state: AnimePlayerMediaState)
}

/**
 * What the viewer asks for from the desktop's controls. Called on a thread of the controls' own, never the UI thread,
 * and only for what the last [AnimePlayerMediaState] offers.
 */
interface AnimePlayerMediaCommands {
    fun play()

    fun pause()

    fun togglePlayback()

    /** Ends the player, as closing it does. */
    fun stop()

    fun seekTo(positionMs: Long)

    fun previousEpisode()

    fun nextEpisode()

    /** Brings the player forward to fill the main window, out of picture-in-picture. */
    fun raise()
}
