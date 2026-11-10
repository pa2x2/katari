package app.katari.telemetry.usage

/**
 * What the app reports of itself once a day: how much it holds and which of its optional parts are set up. Like
 * [ReadingSession], it holds types and counts only.
 */
data class AppState(
    /** Library entries of all profiles together, by the entry type's name. */
    val libraryEntries: Map<String, Int>,
    /** Extensions that are installed and loaded, by the key of their type. */
    val extensions: Map<String, Int>,
    val profiles: Int,
    val sync: SyncUse,
    /** The ID of the translation engine the profile in use chose, null while it uses the device's default. */
    val translation: String?,
    /** The ID of the provider of the read-aloud voice the profile in use chose, null when it has none. */
    val readAloud: String?,
    /** The ID of the text recognition provider the profile in use chose, null when it has none. */
    val textRecognition: String?,
)

/** Whether this device syncs: [Paused] is signed in with sync paused by the user. */
enum class SyncUse { Off, On, Paused }

/** Reads the [AppState] as the day's report goes out, for telemetry, which can't reach the app's data itself. */
fun interface AppStateSource {
    suspend fun read(): AppState
}
