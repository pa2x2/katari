package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.track.TrackRemovalConfirmation
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.AppUiScope
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMutation
import mihon.entry.interactions.tracking.EntryTrackingRecord
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.presentation.kit.field.SelectableDates
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

internal data class TrackDateSelectorScreen(
    private val entry: Entry,
    private val track: EntryTrackingRecord,
    private val service: EntryTrackingServiceDescriptor,
    private val start: Boolean,
) : Screen() {

    @Transient
    private val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val targetDate = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date
            if (targetDate > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date) return false
            return when {
                start && track.finishDate > 0 -> {
                    targetDate <= track.finishDate.toLocalDate()
                }
                !start && track.startDate > 0 -> {
                    track.startDate.toLocalDate() <= targetDate
                }
                else -> true
            }
        }

        override fun isSelectableYear(year: Int): Boolean {
            if (year > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year) return false
            return when {
                start && track.finishDate > 0 -> {
                    year <= track.finishDate.toLocalDate().year
                }
                !start && track.startDate > 0 -> {
                    track.startDate.toLocalDate().year <= year
                }
                else -> true
            }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(entry, track, service, start) }
        val canRemove = if (start) track.startDate > 0 else track.finishDate > 0
        TrackDateSelector(
            title = stringResource(
                if (start) MR.strings.track_started_reading_date else MR.strings.track_finished_reading_date,
            ),
            initialSelectedDateMillis = viewModel.initialSelection,
            selectableDates = selectableDates,
            onConfirm = {
                viewModel.setDate(it)
                navigator.pop()
            },
            onRemove = { viewModel.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    @AssistedInject
    class Model(
        @Assisted private val entry: Entry,
        @Assisted private val track: EntryTrackingRecord,
        @Assisted private val service: EntryTrackingServiceDescriptor,
        @Assisted private val start: Boolean,
        private val trackingFeature: EntryTrackingFeature,
    ) : ViewModel() {

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppUiScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(
                entry: Entry,
                track: EntryTrackingRecord,
                service: EntryTrackingServiceDescriptor,
                start: Boolean,
            ): Model
        }

        val initialSelection: Long
            get() = (if (start) track.startDate else track.finishDate)
                .takeIf { it != 0L }
                ?.convertEpochMillisZone(TimeZone.currentSystemDefault(), TimeZone.UTC)
                ?: Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .atStartOfDayIn(TimeZone.UTC)
                    .toEpochMilliseconds()

        fun setDate(millis: Long) {
            val localMillis = millis.convertEpochMillisZone(TimeZone.UTC, TimeZone.currentSystemDefault())
            mutateDate(localMillis)
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(entry, service, start))
        }

        private fun mutateDate(epochMillis: Long) {
            viewModelScope.launchNonCancellable {
                val mutation = if (start) {
                    EntryTrackingMutation.StartDate(epochMillis)
                } else {
                    EntryTrackingMutation.FinishDate(epochMillis)
                }
                trackingFeature.mutate(entry, service.id, mutation).logFailure("date update")
            }
        }
    }
}

internal data class TrackDateRemoverScreen(
    private val entry: Entry,
    private val service: EntryTrackingServiceDescriptor,
    private val start: Boolean,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(entry, service, start) }
        TrackRemovalConfirmation(
            title = stringResource(MR.strings.track_remove_date_conf_title),
            text = stringResource(
                if (start) {
                    MR.strings.track_remove_start_date_conf_text
                } else {
                    MR.strings.track_remove_finish_date_conf_text
                },
                service.name,
            ),
            confirmLabel = stringResource(MR.strings.action_remove),
            onConfirm = {
                viewModel.removeDate()
                navigator.popUntil { it is TrackInfoDialogHomeScreen }
            },
            onDismissRequest = navigator::pop,
        )
    }

    @AssistedInject
    class Model(
        @Assisted private val entry: Entry,
        @Assisted private val service: EntryTrackingServiceDescriptor,
        @Assisted private val start: Boolean,
        private val trackingFeature: EntryTrackingFeature,
    ) : ViewModel() {

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppUiScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(entry: Entry, service: EntryTrackingServiceDescriptor, start: Boolean): Model
        }

        fun removeDate() {
            viewModelScope.launchNonCancellable {
                val mutation = if (start) EntryTrackingMutation.StartDate(0) else EntryTrackingMutation.FinishDate(0)
                trackingFeature.mutate(entry, service.id, mutation).logFailure("date removal")
            }
        }
    }
}
