package eu.kanade.tachiyomi.ui.stats.recap.delivery

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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

/**
 * The active profile's year recap that is new and not opened yet, while it has activity to show; null otherwise.
 * Drives the marks that point to it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun subscribeUnopenedYearRecap(
    today: LocalDate = LocalDate.now(),
    activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    profileStore: ProfileStore = Injekt.get(),
    recapRepository: StatisticsRecapRepository = Injekt.get(),
): Flow<StatisticsRecapPeriod.Year?> {
    val recap = newYearRecap(today) ?: return flowOf(null)
    return activeProfileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
        combine(
            StatisticsPreferences(profileStore.profileStore(profileId)).lastOpenedYearRecap.changes(),
            recapRepository.subscribeHasActivity(profileId, recap.start.toString(), recap.end.toString()),
        ) { lastOpened, hasActivity -> recap.takeIf { hasActivity && lastOpened != it.editionKey } }
    }
}
