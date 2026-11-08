package mihon.feature.missingextensions.suggestion

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.GlobalSourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.feature.missingextensions.model.MissingExtension
import mihon.feature.missingextensions.model.MissingExtensions
import tachiyomi.core.common.preference.getAndSet

/**
 * What the Library card, the Sync page row, the Extensions tab notice and the first-run guide's last page suggest
 * installing. They count only what can be installed, or trusted where it's installed already: entries from extensions
 * no store lists stay listed in the sheet but never keep a suggestion up. The Library card shows once per extension,
 * when one becomes needed that it hasn't suggested yet, and not while a first sync downloads the library, whose count
 * climbs as titles arrive; the others show while anything can be installed.
 */
@Inject
@SingleIn(AppScope::class)
class MissingExtensionSuggestion(
    private val missing: MissingExtensions,
    private val preferences: GlobalSourcePreferences,
    connection: SyncConnection,
) {
    val installable: Flow<List<MissingExtension>> = if (missing.isInstallOffered) {
        missing.all.map { all -> all.orEmpty().filter { it.cantInstall == null } }
    } else {
        flowOf(emptyList())
    }

    /** Whether a first sync is downloading the library ([SyncStage.isFirstDownload]). */
    val libraryArriving: Flow<Boolean> = connection.states()
        .map { it.isLinked && it.stage.isFirstDownload }
        .distinctUntilChanged()

    /** What the Library card is about; empty while it doesn't show. */
    val libraryCard: Flow<List<MissingExtension>> = combine(
        installable,
        preferences.suggestedMissingExtensions.changes(),
        libraryArriving,
    ) { installable, suggested, arriving ->
        installable.takeIf { extensions -> !arriving && extensions.any { it.key !in suggested } }.orEmpty()
    }

    /** The card was answered, with Not now or by opening the sheet: it stays away until another extension is needed. */
    fun answered(extensions: List<MissingExtension>) {
        preferences.suggestedMissingExtensions.getAndSet { suggested -> suggested + extensions.map { it.key } }
    }
}
