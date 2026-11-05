package mihon.desktop.mpris

import mihon.desktop.dbus.Variant

/**
 * The properties of the two MPRIS interfaces, as `Get` and `GetAll` answer and `PropertiesChanged` reports them.
 * Volume is left out, as the shell isn't offered the player's volume, and so are the optional loop and shuffle ones.
 */
internal object MprisProperties {
    const val ROOT_INTERFACE = "org.mpris.MediaPlayer2"
    const val PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"
    const val POSITION = "Position"

    // The specification's track for a player with nothing to play, as before the first episode's state arrives
    private const val NO_TRACK = "/org/mpris/MediaPlayer2/TrackList/NoTrack"

    fun root(identity: String, desktopEntry: String): Map<String, Variant> = mapOf(
        "CanQuit" to Variant("b", false),
        "CanRaise" to Variant("b", true),
        "HasTrackList" to Variant("b", false),
        "Identity" to Variant("s", identity),
        "DesktopEntry" to Variant("s", desktopEntry),
        "SupportedUriSchemes" to Variant("as", emptyList<String>()),
        "SupportedMimeTypes" to Variant("as", emptyList<String>()),
    )

    /** The player's properties with the position at [positionUs]; shells read it when they want it, never told. */
    fun player(state: MprisPlayerState?, positionUs: Long): Map<String, Variant> {
        val rate = state?.rate ?: 1.0
        return mapOf(
            "PlaybackStatus" to Variant(
                "s",
                when {
                    state == null -> "Stopped"
                    state.playing -> "Playing"
                    else -> "Paused"
                },
            ),
            "Metadata" to Variant("a{sv}", metadata(state)),
            POSITION to Variant("x", positionUs),
            "Rate" to Variant("d", rate),
            // The shell can't change the speed, so the range only spans the speed it plays at and the normal one
            "MinimumRate" to Variant("d", minOf(rate, 1.0)),
            "MaximumRate" to Variant("d", maxOf(rate, 1.0)),
            "CanGoNext" to Variant("b", state?.canGoNext == true),
            "CanGoPrevious" to Variant("b", state?.canGoPrevious == true),
            "CanPlay" to Variant("b", state != null),
            "CanPause" to Variant("b", state != null),
            "CanSeek" to Variant("b", state?.lengthUs != null),
            "CanControl" to Variant("b", true),
        )
    }

    // Shells show the artist under the title, so the series goes there as well as in the album
    private fun metadata(state: MprisPlayerState?): Map<String, Variant> = buildMap {
        put("mpris:trackid", Variant("o", state?.trackId ?: NO_TRACK))
        if (state == null) return@buildMap
        state.lengthUs?.let { put("mpris:length", Variant("x", it)) }
        state.artUrl?.let { put("mpris:artUrl", Variant("s", it)) }
        state.title?.let { put("xesam:title", Variant("s", it)) }
        state.series?.let {
            put("xesam:album", Variant("s", it))
            put("xesam:artist", Variant("as", listOf(it)))
        }
    }
}
