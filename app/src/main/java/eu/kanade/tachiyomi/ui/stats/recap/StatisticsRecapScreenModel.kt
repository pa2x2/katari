package eu.kanade.tachiyomi.ui.stats.recap

import android.graphics.Bitmap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.repository.StatisticsRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.LocalDate

/** @param startLocalDate first day of the period, or null to start at the first recorded activity. */
class StatisticsRecapScreenModel(
    startLocalDate: String?,
    endLocalDate: String,
    typeName: String?,
    activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    statisticsRepository: StatisticsRepository = Injekt.get(),
    statisticsFeature: EntryStatisticsFeature = Injekt.get(),
) : StateScreenModel<StatisticsRecapScreenModel.State>(State.Loading) {

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }

    init {
        screenModelScope.launchIO {
            val recap = try {
                val snapshot = statisticsRepository
                    .subscribeActivity(activeProfileProvider.activeProfileId, startLocalDate, endLocalDate)
                    .first()
                buildStatisticsRecap(snapshot, type, LocalDate.parse(endLocalDate), statisticsFeature.contributions)
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error)
                null
            }
            mutableState.update { recap?.let(State::Success) ?: State.Failed }
        }
    }

    suspend fun writeImage(bitmap: Bitmap, directory: File): File = withIOContext {
        directory.mkdirs()
        // A captured layer may come back as a hardware bitmap, which can't be encoded directly.
        val encodable = if (bitmap.config ==
            Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        File(directory, IMAGE_FILE_NAME).also { file ->
            file.outputStream().use { encodable.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    sealed interface State {
        data object Loading : State

        data object Failed : State

        data class Success(val recap: StatisticsRecap) : State
    }

    private companion object {
        const val IMAGE_FILE_NAME = "statistics_recap.png"
    }
}
