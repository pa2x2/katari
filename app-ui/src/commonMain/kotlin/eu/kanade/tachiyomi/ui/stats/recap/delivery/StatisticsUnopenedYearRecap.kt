package eu.kanade.tachiyomi.ui.stats.recap.delivery

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import java.time.LocalDate

/**
 * The active profile's year recap that is new and not opened yet, while it has activity to show; null otherwise.
 * Drives the marks that point to it.
 */
@Inject
class StatisticsUnopenedYearRecap(
    private val activeProfileProvider: ActiveProfileProvider,
    private val profileStore: ProfileStore,
    private val recapRepository: StatisticsRecapRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe(today: LocalDate = LocalDate.now()): Flow<StatisticsRecapPeriod.Year?> {
        val recap = newYearRecap(today) ?: return flowOf(null)
        return activeProfileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                StatisticsPreferences(profileStore.profileStore(profileId)).lastOpenedYearRecap.changes(),
                recapRepository.subscribeHasActivity(profileId, recap.start.toString(), recap.end.toString()),
            ) { lastOpened, hasActivity -> recap.takeIf { hasActivity && lastOpened != it.editionKey } }
        }
    }
}
