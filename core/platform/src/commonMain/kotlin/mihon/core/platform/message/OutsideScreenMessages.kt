package mihon.core.platform.message

/**
 * Short messages from work that no screen asked for or waits on, such as a tracker failing to send a read chapter.
 * Android shows them as toasts; desktop on the screen the user is on, as the app's own messages.
 */
interface OutsideScreenMessages {
    /** Shows [text], for longer when [long]. Called from any thread; returns once the message is shown or queued. */
    suspend fun show(text: String, long: Boolean = false)
}
