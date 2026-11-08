package app.katari.video.engine.mpv.client

import app.katari.video.engine.spi.player.VideoOutput

/**
 * One player's mpv instance, from opening to [close]: the platform creates it for the session's [MpvSessionHost],
 * which sets it up, and destroys it when the session closes. The instance is driven from one mpv thread of the
 * session's own, so calls into it never block the UI thread; its events come on mpv's event thread.
 */
interface MpvSession {
    /** Runs [block] on the mpv thread with the instance, once it exists; dropped if the session closes first. */
    fun post(block: (MpvClient) -> Unit)

    /**
     * Has mpv draw into [output] from now on, or nowhere for null, where it decodes on without a picture, which is how
     * the sound plays on in the background. An output of a kind the platform's mpv can't draw into stays undrawn.
     */
    fun setVideoOutput(output: VideoOutput?)

    /**
     * Lets go of the video output and destroys the instance, or stops waiting for it, then runs [then] on the mpv
     * thread.
     */
    fun close(then: () -> Unit = {})
}

/**
 * What a [MpvSession] runs its instance for. The lifecycle calls come on the mpv thread, in order; the events of
 * [MpvEventListener] on mpv's event thread.
 */
interface MpvSessionHost : MpvEventListener {
    /**
     * Gets ready for the instance, such as by starting what it plays from; false when it can't run, which ends the
     * session with [failedToStart].
     */
    fun beforeCreate(): Boolean

    /** Sets the new instance's options, before it initialises. */
    fun configure(client: MpvClient)

    /** The instance runs: observe its properties. Anything posted while the session waited runs right after. */
    fun started(client: MpvClient)

    fun failedToStart()
}
