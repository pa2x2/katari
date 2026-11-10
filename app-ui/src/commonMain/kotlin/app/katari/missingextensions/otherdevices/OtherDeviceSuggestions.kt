package app.katari.missingextensions.otherdevices

import app.katari.core.metro.AppCoroutineScope
import app.katari.missingextensions.model.MissingExtensions
import app.katari.missingextensions.model.cantInstall
import app.katari.missingextensions.suggestion.MissingExtensionSuggestion
import app.katari.sync.client.extension.OtherDeviceExtensions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.repository.SourceRepository
import kotlin.time.Duration.Companion.seconds

/**
 * What the account's other devices installed or uninstalled that this device is offered to do too
 * ([OtherDeviceExtensions]): installs of extensions an added store lists in a version this device can run, and
 * uninstalls of extensions installed here. An extension the library's titles need, or that is installed here but not
 * trusted while titles use it, is the missing-extensions sheet's own, so it isn't offered twice. The Library card
 * shows each change once, and not while a first sync downloads the library.
 */
@Inject
@SingleIn(AppScope::class)
class OtherDeviceSuggestions(
    otherDevices: OtherDeviceExtensions,
    private val extensions: ExtensionManagement,
    private val compatibility: ExtensionCompatibility,
    missing: MissingExtensions,
    missingSuggestion: MissingExtensionSuggestion,
    sourceRepository: SourceRepository,
    private val preferences: GlobalSourcePreferences,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val installedHere = combine(extensions.loaded, extensions.notLoaded) { loaded, notLoaded ->
        (loaded + notLoaded).associateBy { it.pkgName }
    }

    private val declined = combine(
        preferences.declinedOtherDeviceInstalls.changes(),
        preferences.declinedOtherDeviceUninstalls.changes(),
        ::Declined,
    )

    /** Null until it's known. */
    val changes: StateFlow<OtherDeviceChanges?> = combine(
        // Listings are only read now and then, so an install to offer is what reads them
        otherDevices.subscribe()
            .onEach { changes -> if (changes.any { it.installed }) scope.launch { extensions.fetchListingsOnce() } },
        installedHere,
        extensions.available,
        missing.all,
        combine(sourceRepository.getLibraryCountsBySource(), declined, ::Pair),
    ) { changes, here, available, missingNow, (counts, declinedNow) ->
        val missingKeys = missingNow.orEmpty().mapTo(HashSet()) { it.key }
        val offered = changes.filter { it.pkgName !in missingKeys }
        val listings = available.groupBy { it.pkgName }
        OtherDeviceChanges(
            installs = offered
                .filter { it.installed && it.pkgName !in here && it.pkgName !in declinedNow.installs }
                .takeIf { extensions.isInstallOffered }
                .orEmpty()
                .mapNotNull { change ->
                    val listing = listings[change.pkgName]
                        ?.firstOrNull { compatibility.cantInstall(it) == null }
                        ?: return@mapNotNull null
                    OtherDeviceExtension(change, listing.name, listing, installed = null, countsByProfile = emptyMap())
                }
                .sortedBy { it.name },
            uninstalls = offered
                .filter { !it.installed && it.pkgName !in declinedNow.uninstalls }
                .mapNotNull { change ->
                    val installed = here[change.pkgName] ?: return@mapNotNull null
                    OtherDeviceExtension(change, installed.name, listing = null, installed, countsOf(installed, counts))
                }
                .sortedBy { it.name },
        )
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), null)

    /** What the Library card is about; empty while it doesn't show. */
    val libraryCard: Flow<OtherDeviceChanges> = combine(
        changes,
        preferences.suggestedOtherDeviceChanges.changes(),
        missingSuggestion.libraryArriving,
    ) { changes, suggested, arriving ->
        changes?.takeIf { !arriving && it.all.any { extension -> extension.changeKey !in suggested } }
            ?: OtherDeviceChanges.NONE
    }

    /** The card was answered, with Not now or by opening the sheet: it stays away until another change comes. */
    fun answered(changes: OtherDeviceChanges) {
        preferences.suggestedOtherDeviceChanges.getAndSet { suggested -> suggested + changes.all.map { it.changeKey } }
    }

    /** [installs] are never suggested again. */
    fun declineInstalls(installs: List<OtherDeviceExtension>) {
        preferences.declinedOtherDeviceInstalls.getAndSet { names -> names + installs.map { it.pkgName } }
    }

    /** [uninstalls] are never suggested again. */
    fun declineUninstalls(uninstalls: List<OtherDeviceExtension>) {
        preferences.declinedOtherDeviceUninstalls.getAndSet { names -> names + uninstalls.map { it.pkgName } }
    }

    fun uninstall(uninstalls: List<OtherDeviceExtension>) {
        uninstalls.mapNotNull { it.installed }.forEach(extensions::uninstall)
    }

    // An extension that isn't loaded provides no sources, so none of its titles are known
    private fun countsOf(installed: Extension.Installed, counts: Map<Long, Map<Long, Long>>): Map<Long, Long> =
        (installed as? Extension.Loaded)?.sources.orEmpty()
            .mapNotNull { counts[it.id] }
            .flatMap { it.entries }
            .groupingBy { it.key }
            .fold(0L) { total, entry -> total + entry.value }

    private class Declined(val installs: Set<String>, val uninstalls: Set<String>)
}
