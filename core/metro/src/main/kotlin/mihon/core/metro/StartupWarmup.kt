package mihon.core.metro

/**
 * Work a module starts once the app has launched, on the main thread right after `Application.onCreate`, so it is
 * ready before the first screen that needs it. Contribute it with `@IntoSet`.
 */
fun interface StartupWarmup {
    fun warmUp()
}
