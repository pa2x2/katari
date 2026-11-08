package eu.kanade.tachiyomi.ui.entry.translation

import app.katari.core.platform.PlatformContext
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.ModelArtifactState
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.provider.TranslationProviderDisclosure
import kotlinx.coroutines.flow.Flow

/**
 * Where the fixes of the translation setup sheet are carried out: the platform's translation engines, text recognition
 * and model downloads, and the settings screens for what the sheet cannot settle itself.
 */
interface EntryTranslateSetupHost {
    fun recordRecentLanguage(language: LanguageTag)

    /** Starts a model download the user approved; [observeModel] reports when it is installed. */
    fun downloadModel(approval: ModelArtifactDownloadApproval)

    fun observeModel(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState>

    suspend fun installPlatformModels(component: TextRecognitionComponentId, language: LanguageTag)

    suspend fun acknowledgeDisclosure(engine: TranslationEngineId, disclosure: TranslationProviderDisclosure)

    suspend fun downloadTranslationModels(engine: TranslationEngineId, models: List<TranslationModelDescriptor>)

    suspend fun openTranslationSetup(engine: TranslationEngineId)

    /** Lets the user choose how [language] is read; [context] is where the choice opens from. */
    fun openPipelineChoice(context: PlatformContext, language: LanguageTag)

    fun openTranslationSettings(context: PlatformContext)
}
