package tachiyomi.domain.entry.model

/** The player a title always plays in, chosen by the viewer; without one, the player picks for each episode. */
enum class PlayerEngineOverride {
    BUILT_IN,
    MPV,
}
