package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.lang.toLocalDate
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingRecord
import mihon.entry.interactions.EntryTrackingServiceDescriptor
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.AlertDialogContent
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal data class TrackDateSelectorScreen(
    private val entry: Entry,
    private val track: EntryTrackingRecord,
    private val service: EntryTrackingServiceDescriptor,
    private val start: Boolean,
) : Screen() {

    @Transient
    private val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val targetDate = Instant.ofEpochMilli(utcTimeMillis).toLocalDate(ZoneOffset.UTC)
            if (targetDate > LocalDate.now(ZoneOffset.UTC)) return false
            return when {
                start && track.finishDate > 0 -> {
                    targetDate <= Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC)
                }
                !start && track.startDate > 0 -> {
                    Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC) <= targetDate
                }
                else -> true
            }
        }

        override fun isSelectableYear(year: Int): Boolean {
            if (year > LocalDate.now(ZoneOffset.UTC).year) return false
            return when {
                start && track.finishDate > 0 -> {
                    year <= Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC).year
                }
                !start && track.startDate > 0 -> {
                    Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC).year <= year
                }
                else -> true
            }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { Model(entry, track, service, start) }
        val canRemove = if (start) track.startDate > 0 else track.finishDate > 0
        TrackDateSelector(
            title = stringResource(
                if (start) MR.strings.track_started_reading_date else MR.strings.track_finished_reading_date,
            ),
            initialSelectedDateMillis = screenModel.initialSelection,
            selectableDates = selectableDates,
            onConfirm = {
                screenModel.setDate(it)
                navigator.pop()
            },
            onRemove = { screenModel.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val entry: Entry,
        private val track: EntryTrackingRecord,
        private val service: EntryTrackingServiceDescriptor,
        private val start: Boolean,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : ScreenModel {

        val initialSelection: Long
            get() = (if (start) track.startDate else track.finishDate)
                .takeIf { it != 0L }
                ?.convertEpochMillisZone(ZoneOffset.systemDefault(), ZoneOffset.UTC)
                ?: Instant.now().toEpochMilli()

        fun setDate(millis: Long) {
            val localMillis = millis.convertEpochMillisZone(ZoneOffset.UTC, ZoneOffset.systemDefault())
            mutateDate(localMillis)
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(entry, service, start))
        }

        private fun mutateDate(epochMillis: Long) {
            screenModelScope.launchNonCancellable {
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

private data class TrackDateRemoverScreen(
    private val entry: Entry,
    private val service: EntryTrackingServiceDescriptor,
    private val start: Boolean,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { Model(entry, service, start) }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = {
                Text(
                    text = stringResource(MR.strings.track_remove_date_conf_title),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        if (start) {
                            MR.strings.track_remove_start_date_conf_text
                        } else {
                            MR.strings.track_remove_finish_date_conf_text
                        },
                        service.name,
                    ),
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.End),
                ) {
                    TextButton(onClick = navigator::pop) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    FilledTonalButton(
                        onClick = {
                            screenModel.removeDate()
                            navigator.popUntil { it is TrackInfoDialogHomeScreen }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(text = stringResource(MR.strings.action_remove))
                    }
                }
            },
        )
    }

    private class Model(
        private val entry: Entry,
        private val service: EntryTrackingServiceDescriptor,
        private val start: Boolean,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : ScreenModel {

        fun removeDate() {
            screenModelScope.launchNonCancellable {
                val mutation = if (start) EntryTrackingMutation.StartDate(0) else EntryTrackingMutation.FinishDate(0)
                trackingFeature.mutate(entry, service.id, mutation).logFailure("date removal")
            }
        }
    }
}
