package mihon.webview.jcef.runtime

/**
 * Chromium's switches: the JetBrains Runtime's own ([runtimeSwitches], from `JCefAppConfig`) with its scale replaced,
 * and the app's. Chromium keeps the last value of a repeated switch, so each switch here appears once.
 */
internal fun chromiumSwitches(runtimeSwitches: List<String>, display: JcefDisplay, deviceScaleFactor: Double) =
    runtimeSwitches.filterNot { it.startsWith(DEVICE_SCALE_FACTOR) } + listOf(
        // The runtime passes AWT's scale, which on X11 is a whole number (1 at 125 %) while the app applies the
        // session's scale itself.
        "$DEVICE_SCALE_FACTOR$deviceScaleFactor",
        // Chromium otherwise picks its display system from the environment: native Wayland whenever the session is
        // Wayland, even when the app's windows go through XWayland.
        "--ozone-platform=${display.ozonePlatform}",
        // Extensions installed for the system's Chromium, such as a desktop's browser integration in
        // /usr/share/chromium/extensions, would otherwise load into every page.
        "--disable-extensions",
        // Chrome's components (Widevine, safety lists, on-device models, a speech engine) would otherwise be fetched,
        // about 130 MB, for features web views don't use.
        "--disable-component-update",
        // Chromium would otherwise encrypt its cookies and passwords with a key kept in the desktop's keyring, which
        // can ask the user to unlock it; the app's own cookie store is the one that counts and keeps no such key.
        "--password-store=basic",
    )

private const val DEVICE_SCALE_FACTOR = "--force-device-scale-factor="

private val JcefDisplay.ozonePlatform: String
    get() = when (this) {
        JcefDisplay.X11 -> "x11"
        JcefDisplay.WAYLAND -> "wayland"
    }
