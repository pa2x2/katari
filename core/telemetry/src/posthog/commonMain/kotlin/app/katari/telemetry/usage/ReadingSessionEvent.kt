package app.katari.telemetry.usage

/** The "Reading session ended" event's properties: a property is left out where its value doesn't apply. */
internal object ReadingSessionEvent {
    const val NAME = "Reading session ended"

    fun properties(session: ReadingSession): Map<String, Any> = buildMap {
        put("entry_type", session.entryType)
        session.readingMode?.let { put("reading_mode", it) }
        session.playerEngine?.let { put("player_engine", it) }
        session.engineSwitched?.let { put("engine_switched", it) }
        session.sourceKind?.let { put("source_kind", it) }
        session.contentOrigin?.let { put("content_origin", it.name.lowercase()) }
        put("duration_seconds", session.duration.inWholeSeconds)
        put("finished", session.finished)
        session.openTime?.let { put("open_time_ms", it.inWholeMilliseconds) }
    }
}
