package eu.kanade.presentation.entry.translation

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.translate.EntryTranslatePreparation
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.provider.TranslationProviderDisclosure
import tachiyomi.domain.entry.model.Entry

/**
 * Chapters waiting for their translation prerequisites. Each entry among them has its own languages, so each is
 * prepared on its own; the chapters queue once all of them are [ready].
 *
 * @property working set while a fix the user started, such as a model download, is still running.
 */
data class EntryTranslateSetupState(
    val items: List<TranslatableChapter>,
    val preparations: Map<Entry, EntryTranslatePreparation>,
    val working: Boolean,
) {
    val ready: Map<Entry, EntryTranslatePreparation.Ready>?
        get() = preparations.mapValues { it.value as? EntryTranslatePreparation.Ready ?: return null }
}

/** What the prerequisites sheet can do to settle what blocks translation. */
interface EntryTranslateSetupActions {
    fun dismissSetup()

    /** Prepares again, for example after the user returned from settings. */
    fun refreshSetup()

    fun confirmSetup()

    fun choosePageLanguage(entry: Entry, language: LanguageTag)

    fun chooseTargetLanguage(entry: Entry, language: LanguageTag)

    fun approveRecognitionModels(approvals: List<ModelArtifactDownloadApproval>)

    fun installPlatformModels(component: TextRecognitionComponentId, language: LanguageTag)

    fun acknowledgeDisclosure(engine: TranslationEngineId, disclosure: TranslationProviderDisclosure)

    fun downloadTranslationModels(engine: TranslationEngineId, models: List<TranslationModelDescriptor>)

    fun openTranslationSetup(engine: TranslationEngineId)

    /** Leaves for the choice of how [language] is read; the sheet prepares again when the user returns. */
    fun choosePipeline(context: PlatformContext, language: LanguageTag)

    fun openTranslationSettings(context: PlatformContext)
}
