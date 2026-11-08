package app.katari.desktop.window

import app.katari.core.metro.BasePreferenceStore
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.awt.Rectangle

/**
 * Where the app's windows were when the app last ran, on the base store since the windows stay through a profile
 * switch. App state, so backups leave it out: it belongs to this desktop's screens.
 */
@Inject
class WindowPreferences(@BasePreferenceStore store: PreferenceStore) {
    /**
     * The window's place and size, in the units AWT places windows in, the last time it was neither maximised nor
     * minimised; null before the first start.
     */
    val floatingBounds: Preference<Rectangle?> = store.bounds("window_floating_bounds")

    /** Where and how large the player's picture-in-picture window last was, in AWT's units; null before it first opens. */
    val pictureInPictureBounds: Preference<Rectangle?> = store.bounds("window_picture_in_picture_bounds")

    val maximized: Preference<Boolean> = store.getBoolean(Preference.appStateKey("window_maximized"), false)
}

private fun PreferenceStore.bounds(key: String): Preference<Rectangle?> = getObjectFromString(
    key = Preference.appStateKey(key),
    defaultValue = null,
    serializer = { bounds -> bounds?.run { "$x,$y,$width,$height" }.orEmpty() },
    deserializer = { value ->
        value.split(',').mapNotNull(String::toIntOrNull).takeIf { it.size == 4 }
            ?.let { (x, y, width, height) -> Rectangle(x, y, width, height) }
    },
)
