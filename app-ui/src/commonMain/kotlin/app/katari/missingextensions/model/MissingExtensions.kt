package app.katari.missingextensions.model

import app.katari.core.metro.AppCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.SourceRepository
import kotlin.time.Duration.Companion.seconds

/**
 * The extensions the libraries of every profile need that aren't installed on this device, or aren't trusted in this
 * app, as sync, a restore or an uninstall left them. Extensions are installed for the whole device, so every profile's
 * entries count.
 *
 * An untrusted extension never loaded, so its sources are unknown: the store listing that provides a source names its
 * package, and so does the source registry where the extension provided it here before.
 */
@Inject
@SingleIn(AppScope::class)
class MissingExtensions(
    sourceRepository: SourceRepository,
    private val extensions: ExtensionManagement,
    private val sourceIdentities: SourceIdentityRepository,
    compatibility: ExtensionCompatibility,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    /** Null until it's known. */
    val all: StateFlow<List<MissingExtension>?> = combine(
        // Listings are only read now and then, so entries from sources not installed are what reads them
        sourceRepository.getMissingLibrarySources()
            .onEach { if (it.isNotEmpty()) scope.launch { extensions.fetchListingsOnce() } },
        extensions.listingsBySource,
        extensions.notLoaded,
    ) { sources, listings, notLoaded ->
        val untrusted = notLoaded
            .filter { it.reason is Extension.NotLoaded.Reason.Untrusted }
            .associateBy { it.pkgName }
        val providers = if (untrusted.isNotEmpty() && sources.any { it.source.id !in listings }) {
            sourceIdentities.findAll()
                .mapNotNull { source -> source.provider?.packageName?.let { source.localKey to it } }
                .toMap()
        } else {
            emptyMap()
        }
        val needed = sources.map {
            val listing = listings[it.source.id]
            NeededSource(
                key = it.source.id,
                name = it.source.visualName(),
                listing = listing,
                untrusted = (listing?.pkgName ?: providers[it.source.id])?.let(untrusted::get),
                countsByProfile = it.countsByProfile,
            )
        }
        missingExtensions(needed) { compatibility.cantInstall(it) }
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), null)

    /** Whether this platform installs extensions at all; where it doesn't, nothing suggests installing them. */
    val isInstallOffered: Boolean
        get() = extensions.isInstallOffered
}
