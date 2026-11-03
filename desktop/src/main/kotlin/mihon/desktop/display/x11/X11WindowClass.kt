package mihon.desktop.display.x11

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.awt.Toolkit
import java.lang.reflect.InaccessibleObjectException

/**
 * X11: the class in every window's `WM_CLASS`, which desktops match against an app's desktop entry, by its file name or
 * its `StartupWMClass`, to show the app's name and icon for its windows and group them under it. The X11 toolkit names
 * it after the main class (`mihon-desktop-MainKt`) and has no setting for it, so it is set on the toolkit's field,
 * which the app's `--add-opens java.desktop/sun.awt.X11` opens. Windows read it as they open.
 */
internal object X11WindowClass {
    /** Starts the toolkit, so it runs only once the window toolkit is chosen. */
    fun set(name: String) {
        val toolkit = Toolkit.getDefaultToolkit()
        if (toolkit.javaClass.name != X11_TOOLKIT) return
        try {
            toolkit.javaClass.getDeclaredField(CLASS_NAME_FIELD).apply { isAccessible = true }.set(null, name)
        } catch (e: ReflectiveOperationException) {
            logcat(LogPriority.WARN, e) { "Couldn't name the windows' class $name" }
        } catch (e: InaccessibleObjectException) {
            logcat(LogPriority.WARN, e) { "Couldn't name the windows' class $name" }
        }
    }

    private const val X11_TOOLKIT = "sun.awt.X11.XToolkit"
    private const val CLASS_NAME_FIELD = "awtAppClassName"
}
