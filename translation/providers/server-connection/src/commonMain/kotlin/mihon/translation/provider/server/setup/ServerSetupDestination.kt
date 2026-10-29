package mihon.translation.provider.server.setup

/**
 * Opens a provider's setup screen from outside any screen, as an engine's setup does when the translation settings or
 * a reader ask for it: Android in the provider's own setup activity, desktop on the window's navigator.
 */
fun interface ServerSetupDestination {
    /** False when there is nowhere to show [screen]. */
    suspend fun open(screen: ProviderSetupScreen): Boolean
}
