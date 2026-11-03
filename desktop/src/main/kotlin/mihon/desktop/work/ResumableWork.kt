package mihon.desktop.work

/**
 * Work that the app starts again at its next launch when it quit before the work ended, as WorkManager keeps work
 * across restarts on Android. [PersistedWork] keeps what is pending until then.
 */
interface ResumableWork {
    /** The [InProcessWorkRequest.resumeAs] kinds this starts again. */
    val kinds: Set<String>

    /** Requests the work of [kind] again with [input], the way it was requested before the app quit. */
    suspend fun resume(kind: String, input: Map<String, String>)
}
