package app.katari.telemetry.usage

import kotlin.time.Duration

/**
 * One time through a reader or the player, from opening it to closing it. Every value is a type, a setting or a count:
 * nothing names what was read, or the source or extension it came from.
 */
data class ReadingSession(
    /** The entry type's name: manga, anime or book. */
    val entryType: String,
    /** The reading mode's setting value at close; null for the player, which has none. */
    val readingMode: String?,
    /** The engine playing at close; null for the readers. */
    val playerEngine: String?,
    /** Whether the player changed engine during the session; null for the readers. */
    val engineSwitched: Boolean?,
    /** The key of the extension type the source comes from; null where it isn't known, such as a missing extension. */
    val sourceKind: String?,
    /** Null when the session closed before showing anything. */
    val contentOrigin: ContentOrigin?,
    val duration: Duration,
    /** The chapters or episodes completed during the session, each counted once. */
    val finished: Int,
    /** From opening to the first page, text or frame shown; null when nothing was shown before close. */
    val openTime: Duration?,
)

/** Where what a session showed came from: [Mixed] when it showed both downloaded and streamed chapters. */
enum class ContentOrigin { Downloaded, Streamed, Mixed }
