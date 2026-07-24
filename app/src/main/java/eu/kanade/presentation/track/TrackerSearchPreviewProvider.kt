package eu.kanade.presentation.track

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import mihon.entry.interactions.EntryTrackingSearchCandidate
import mihon.entry.interactions.EntryTrackingServiceId
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.random.Random

internal class TrackerSearchPreviewProvider : PreviewParameterProvider<@Composable () -> Unit> {
    private val fullPageWithSecondSelected = @Composable {
        val items = someTrackSearches().take(30).toList()
        TrackerSearch(
            state = TextFieldState(initialText = "search text"),
            onDispatchQuery = {},
            queryResult = Result.success(items),
            selected = items[1],
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val fullPageWithoutSelected = @Composable {
        TrackerSearch(
            state = TextFieldState(),
            onDispatchQuery = {},
            queryResult = Result.success(someTrackSearches().take(30).toList()),
            selected = null,
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val loading = @Composable {
        TrackerSearch(
            state = TextFieldState(),
            onDispatchQuery = {},
            queryResult = null,
            selected = null,
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val fullPageWithPrivateTracking = @Composable {
        val items = someTrackSearches().take(30).toList()
        TrackerSearch(
            state = TextFieldState(initialText = "search text"),
            onDispatchQuery = {},
            queryResult = Result.success(items),
            selected = items[1],
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = true,
        )
    }
    override val values: Sequence<@Composable () -> Unit> = sequenceOf(
        fullPageWithSecondSelected,
        fullPageWithoutSelected,
        loading,
        fullPageWithPrivateTracking,
    )

    private fun someTrackSearches(): Sequence<EntryTrackingSearchCandidate> = sequence {
        while (true) {
            yield(randTrackSearch())
        }
    }

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun randTrackSearch() = EntryTrackingSearchCandidate(
        serviceId = EntryTrackingServiceId(Random.nextLong()),
        localId = Random.nextLong(),
        entryId = Random.nextLong(),
        remoteId = Random.nextLong(),
        libraryId = Random.nextLong(),
        title = lorem((1..10).random()).joinToString(),
        progress = (0..100).random().toDouble(),
        total = (100L..1000L).random(),
        score = (0..10).random().toDouble(),
        status = Random.nextLong(),
        startDate = 0L,
        finishDate = 0L,
        private = false,
        remoteUrl = "https://example.com/tracker-example",
        coverUrl = "https://example.com/cover.png",
        publicationStartDate = formatter.format(Date.from(Instant.now().minus((1L..365).random(), ChronoUnit.DAYS))),
        summary = lorem((0..40).random()).joinToString(),
        publishingStatus = if (Random.nextBoolean()) "Finished" else "",
        publishingType = if (Random.nextBoolean()) "Oneshot" else "",
        artists = randomNames(),
        authors = randomNames(),
    )

    private fun randomNames(): List<String> = (0..(0..3).random()).map { lorem((3..5).random()).joinToString() }

    private fun lorem(words: Int): Sequence<String> =
        LoremIpsum(words).values
}
