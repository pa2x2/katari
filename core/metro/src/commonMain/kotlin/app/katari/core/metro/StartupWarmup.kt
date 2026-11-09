package app.katari.core.metro

/**
 * Work a module starts once the app's process has started, so it is ready before the first screen that needs it: on
 * Android on the main thread right after `Application.onCreate`, and on desktop on a thread of its own once the
 * extension host runs. Contribute it with `@IntoSet`.
 */
fun interface StartupWarmup {
    fun warmUp()
}
