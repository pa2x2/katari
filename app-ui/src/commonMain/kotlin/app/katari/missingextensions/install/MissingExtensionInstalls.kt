package app.katari.missingextensions.install

import app.katari.core.metro.AppCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Installs the extensions picked in the missing-extensions sheet one after another, so the system asks about one at a
 * time. It belongs to the app rather than the sheet: an install stops when nothing follows it, and the queue keeps
 * going after the sheet closes.
 */
@Inject
@SingleIn(AppScope::class)
class MissingExtensionInstalls(
    private val extensions: ExtensionManagement,
    @AppCoroutineScope scope: CoroutineScope,
) {
    private val queue = Channel<Extension.Available>(Channel.UNLIMITED)
    private val _steps = MutableStateFlow<Map<String, InstallStep>>(emptyMap())

    /**
     * The step of each extension queued here, by package name: [InstallStep.Pending] while it waits its turn. A
     * finished one keeps its last step, so the sheet can say it was installed or failed.
     */
    val steps: StateFlow<Map<String, InstallStep>> = _steps.asStateFlow()

    init {
        scope.launch {
            for (listing in queue) {
                extensions.install(listing)
                    .takeWhile { step ->
                        _steps.update { it + (listing.pkgName to step) }
                        !step.isCompleted()
                    }
                    .collect {}
            }
        }
    }

    /** Queues [listings], leaving out those queued or being installed already. */
    fun install(listings: List<Extension.Available>) {
        listings.forEach { listing ->
            val step = _steps.value[listing.pkgName]
            if (step != null && !step.isCompleted()) return@forEach
            _steps.update { it + (listing.pkgName to InstallStep.Pending) }
            queue.trySend(listing)
        }
    }
}
