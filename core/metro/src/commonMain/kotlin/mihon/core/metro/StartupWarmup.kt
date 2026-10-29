package mihon.core.metro

/**
 * Work a module starts once the app has launched, so it is ready before the first screen that needs it: on Android on
 * the main thread right after `Application.onCreate`, on desktop before the window opens. Contribute it with `@IntoSet`.
 */
fun interface StartupWarmup {
    fun warmUp()
}
