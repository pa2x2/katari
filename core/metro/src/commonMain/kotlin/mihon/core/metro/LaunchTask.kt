package mihon.core.metro

import kotlinx.coroutines.CoroutineScope

/**
 * Work a module does at each launch of the app, once its profile is open and the home screen shows. The launch waits
 * for [run] to return, and Android keeps its splash screen up until then; work the launch shouldn't wait for is
 * launched in the given scope, which lives as long as what hosts the home screen. Contribute it with
 * `@ContributesIntoSet`.
 */
fun interface LaunchTask {
    suspend fun run(scope: CoroutineScope)
}
