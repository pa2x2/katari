package mihon.desktop.window

import dev.zacsweers.metro.Inject
import mihon.core.metro.BasePreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.awt.Rectangle

/**
 * Where the window was when the app last ran, on the base store since the window stays through a profile switch. App
 * state, so backups leave it out: it belongs to this desktop's screens.
 */
@Inject
class WindowPreferences(@BasePreferenceStore store: PreferenceStore) {
    /**
     * The window's place and size, in the units AWT places windows in, the last time it was neither maximised nor
     * minimised; null before the first start.
     */
    val floatingBounds: Preference<Rectangle?> = store.getObjectFromString(
        key = Preference.appStateKey("window_floating_bounds"),
        defaultValue = null,
        serializer = { bounds -> bounds?.run { "$x,$y,$width,$height" }.orEmpty() },
        deserializer = { value ->
            value.split(',').mapNotNull(String::toIntOrNull).takeIf { it.size == 4 }
                ?.let { (x, y, width, height) -> Rectangle(x, y, width, height) }
        },
    )

    val maximized: Preference<Boolean> = store.getBoolean(Preference.appStateKey("window_maximized"), false)
}
