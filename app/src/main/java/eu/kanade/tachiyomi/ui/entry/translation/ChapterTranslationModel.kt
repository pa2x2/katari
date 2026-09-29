package eu.kanade.tachiyomi.ui.entry.translation

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.EntryTranslateSetupActions
import eu.kanade.presentation.entry.translation.EntryTranslateSetupState
import eu.kanade.presentation.entry.translation.TranslatableChapter
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*

/**
 * Translation of the chapters a screen lists: their statuses, the actions their menus offer, and the sheet that
 * settles prerequisites before chapters are queued.
 *
 * @param download queues the downloads of chapters that are translated once downloaded.
 */
class ChapterTranslationModel(
    private val scope: CoroutineScope,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val download: (List<TranslatableChapter>) -> Unit,
    private val feature: EntryTranslateFeature,
    private val languages: EntryTranslationLanguagesFeature,
    private val recognitionHost: TextRecognitionHostActions,
    private val translationHost: TranslationHostActions,
    private val modelStore: ModelArtifactStore,
) : EntryTranslateSetupActions {
    private val mutableSheet = MutableStateFlow<EntryTranslateSetupState?>(null)

    /** The prerequisites sheet, shown while chapters wait for the user to settle what blocks their translation. */
    val sheet: StateFlow<EntryTranslateSetupState?> = mutableSheet.asStateFlow()

    private var modelWait: Job? = null

    fun isApplicable(type: EntryType): Boolean = feature.isApplicable(type)

    /** Statuses of the chapters of [entries] that are translated, queued or failed. */
    fun statuses(entries: List<Entry>): Flow<Map<Long, EntryTranslateStatus>> {
        val flows = entries.filter { isApplicable(it.type) }.map(feature::observeStatuses)
        if (flows.isEmpty()) return flowOf(emptyMap())
        return combine(flows) { maps -> maps.fold(emptyMap()) { all, map -> all + map } }
    }

    /** Statuses of every chapter in the translation queue, of any entry. */
    fun queueStatuses(): Flow<Map<Long, EntryTranslateStatus>> =
        feature.queue.map { queue -> queue.associate { it.chapterId to it.status } }

    /** Runs [action] on [items]; [statuses] are the statuses they are shown with. */
    fun run(
        items: List<TranslatableChapter>,
        action: ChapterTranslateAction,
        statuses: Map<Long, EntryTranslateStatus>,
    ) {
        val ids = items.map { it.id }
        when (action) {
            // Chapters already translated or on their way are left alone; failed ones are queued afresh.
            ChapterTranslateAction.TRANSLATE -> queue(
                items.filter { statuses[it.id].let { it == null || it is EntryTranslateStatus.Failed } },
            )
            ChapterTranslateAction.TRANSLATE_AGAIN, ChapterTranslateAction.DOWNLOAD_AND_TRANSLATE -> queue(items)
            ChapterTranslateAction.TRANSLATE_NOW -> scope.launch { feature.startNow(ids) }
            ChapterTranslateAction.CANCEL -> scope.launch { feature.cancel(ids) }
            ChapterTranslateAction.RETRY -> {
                // Chapters that failed for want of setup go through the requirements again, where the user fixes them.
                val (needSetup, others) = items.partition { item ->
                    (statuses[item.id] as? EntryTranslateStatus.Failed)?.failure == EntryTranslateFailure.SetupRequired
                }
                if (needSetup.isNotEmpty()) queue(needSetup)
                if (others.isNotEmpty()) scope.launch { feature.retry(others.map { it.id }) }
            }
            ChapterTranslateAction.DELETE_TRANSLATION -> scope.launch {
                items.groupBy { it.entry }.forEach { (entry, group) ->
                    feature.deleteTranslation(entry, group.map { it.chapter })
                }
            }
        }
    }

    override fun dismissSetup() {
        modelWait?.cancel()
        mutableSheet.value = null
    }

    override fun refreshSetup() {
        val sheet = mutableSheet.value ?: return
        scope.launch {
            prepare(sheet.items)?.let { prepared ->
                mutableSheet.update { it?.let { prepared.copy(working = it.working) } }
            }
        }
    }

    override fun confirmSetup() {
        val sheet = mutableSheet.value ?: return
        val ready = sheet.ready ?: return
        mutableSheet.value = null
        scope.launch { enqueue(sheet.items, ready) }
    }

    override fun choosePageLanguage(entry: Entry, language: LanguageTag) {
        scope.launch {
            languages.setContentLanguage(entry, language)
            refreshSetup()
        }
    }

    override fun chooseTargetLanguage(entry: Entry, language: LanguageTag) {
        scope.launch {
            languages.setTargetLanguage(entry, language)
            translationHost.recordRecentLanguage(language)
            refreshSetup()
        }
    }

    /** Downloads recognition models the user approved and prepares again once all of them are installed. */
    override fun approveRecognitionModels(approvals: List<ModelArtifactDownloadApproval>) {
        approvals.forEach(modelStore::download)
        modelWait?.cancel()
        mutableSheet.update { it?.copy(working = true) }
        modelWait = scope.launch {
            val models = approvals.map { it.artifact }
            combine(models.map(modelStore::observe)) { states -> states.all { it is ModelArtifactState.Installed } }
                .first { it }
            mutableSheet.update { it?.copy(working = false) }
            refreshSetup()
        }
    }

    override fun installPlatformModels(component: TextRecognitionComponentId, language: LanguageTag) {
        working { recognitionHost.installPlatformModels(component, language) }
    }

    override fun acknowledgeDisclosure(engine: TranslationEngineId, disclosure: TranslationProviderDisclosure) {
        working { translationHost.acknowledgeProviderDisclosure(engine, disclosure) }
    }

    override fun downloadTranslationModels(engine: TranslationEngineId, models: List<TranslationModelDescriptor>) {
        working { translationHost.downloadModels(engine, models) }
    }

    override fun openTranslationSetup(engine: TranslationEngineId) {
        scope.launch { translationHost.openSetup(engine) }
    }

    private fun working(action: suspend () -> Unit) {
        scope.launch {
            mutableSheet.update { it?.copy(working = true) }
            try {
                action()
            } finally {
                mutableSheet.update { it?.copy(working = false) }
            }
            refreshSetup()
        }
    }

    private fun queue(items: List<TranslatableChapter>) {
        if (items.isEmpty()) return
        scope.launch {
            val prepared = prepare(items) ?: return@launch
            val ready = prepared.ready
            if (ready != null) {
                enqueue(items, ready)
            } else {
                mutableSheet.value = prepared
            }
        }
    }

    private suspend fun prepare(items: List<TranslatableChapter>): EntryTranslateSetupState? {
        val preparations = items.map { it.entry }.distinctBy { it.id }
            .associateWith { entry -> feature.prepare(entry) ?: return null }
        return EntryTranslateSetupState(items, preparations, working = false)
    }

    private suspend fun enqueue(
        items: List<TranslatableChapter>,
        ready: Map<Entry, EntryTranslatePreparation.Ready>,
    ) {
        items.groupBy { it.entry }.forEach { (entry, group) ->
            val setup = ready.getValue(entry).setup
            feature.translate(entry, group.map { it.chapter }, setup, startNow = false)
        }
        // Translation items wait for downloads queued after them, so they see those downloads finish.
        download(
            items.filter {
                it.downloadState == EntryDownloadState.NOT_DOWNLOADED || it.downloadState == EntryDownloadState.ERROR
            },
        )
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.pluralStringResource(MR.plurals.chapter_translation_queued, items.size, items.size),
                actionLabel = context.stringResource(MR.strings.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) feature.cancel(items.map { it.id })
        }
    }
}
