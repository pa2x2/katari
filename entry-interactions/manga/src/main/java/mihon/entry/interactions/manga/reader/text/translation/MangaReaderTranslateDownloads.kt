package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * The series' "Translate downloads" setting as the reader's languages sheet shows it, and the offer to translate the
 * chapters that were downloaded before it was turned on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MangaReaderTranslateDownloads(
    private val scope: CoroutineScope,
    private val series: StateFlow<Entry?>,
    private val languages: EntryTranslationLanguagesFeature,
    private val translate: EntryTranslateFeature,
) {
    /** Whether the series translates its downloads; null when its chapters cannot be translated in the background. */
    val enabled: StateFlow<Boolean?> = series
        .filterNotNull()
        .distinctUntilChangedBy(Entry::id)
        .flatMapLatest { entry ->
            if (translate.isApplicable(entry.type)) {
                languages.observe(entry).map { it.translateDownloads }
            } else {
                flowOf(null)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val offered = MutableStateFlow<Offer?>(null)

    /** Downloaded chapters that are not translated, offered right after the setting is turned on. */
    val offer: StateFlow<Offer?> = offered.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        val entry = series.value ?: return
        scope.launch {
            languages.setTranslateDownloads(entry, enabled)
            offered.value = if (enabled) {
                translate.untranslatedDownloads(entry).takeIf { it.isNotEmpty() }?.let { Offer(entry, it) }
            } else {
                null
            }
        }
    }

    fun acceptOffer() {
        val offer = offered.value ?: return
        offered.value = null
        scope.launch { translate.translateWithCurrentSettings(offer.entry, offer.chapters) }
    }

    fun declineOffer() {
        offered.value = null
    }

    class Offer(val entry: Entry, val chapters: List<EntryChapter>)
}
