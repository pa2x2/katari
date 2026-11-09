package app.katari.telemetry

/**
 * What the app shows, which every event carries so that a crash report says where it happened. [entryType] is the type
 * of the entry the screen is about, null where it is about none; [density] is the window's pixels per dp.
 */
internal data class ShownScreen(val screen: String, val entryType: String?, val density: Float) {
    val properties: Map<String, Any>
        get() = buildMap {
            put("screen", screen)
            entryType?.let { put("entry_type", it) }
            put("density", density)
        }
}
