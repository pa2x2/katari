package mihon.feature.missingextensions.model

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import tachiyomi.domain.source.repository.SourceRepository
import kotlin.time.Duration.Companion.seconds

/**
 * The extensions the libraries of every profile need that aren't installed on this device, as sync, a restore or an
 * uninstall left them. Extensions are installed for the whole device, so every profile's entries count.
 */
@Inject
@SingleIn(AppScope::class)
class MissingExtensions(
    sourceRepository: SourceRepository,
    private val extensions: ExtensionManagement,
    compatibility: ExtensionCompatibility,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    /** Null until it's known. */
    val all: StateFlow<List<MissingExtension>?> = combine(
        // Listings are only read now and then, so entries from sources not installed are what reads them
        sourceRepository.getMissingLibrarySources()
            .onEach { if (it.isNotEmpty()) scope.launch { extensions.fetchListingsOnce() } },
        extensions.listingsBySource,
    ) { sources, listings ->
        val needed = sources.map {
            NeededSource(it.source.id, it.source.visualName(), listings[it.source.id], it.countsByProfile)
        }
        missingExtensions(needed) { compatibility.cantInstall(it) }
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), null)

    /** Whether this platform installs extensions at all; where it doesn't, nothing suggests installing them. */
    val isInstallOffered: Boolean
        get() = extensions.isInstallOffered
}
