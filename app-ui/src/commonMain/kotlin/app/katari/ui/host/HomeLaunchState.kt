package app.katari.ui.host

/**
 * Whether the app's launch has run in a window: [HomeHost] runs it on the first start of the window's navigator, and
 * not again when the navigator starts for another profile. A window recreated with its saved state, such as an Android
 * activity after a configuration change, passes back the [isCompleted] it saved.
 */
class HomeLaunchState(isCompleted: Boolean = false) {
    var isCompleted: Boolean = isCompleted
        internal set
}
