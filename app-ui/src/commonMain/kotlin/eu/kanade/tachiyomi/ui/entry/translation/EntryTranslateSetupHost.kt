package eu.kanade.tachiyomi.ui.entry.translation

import kotlinx.coroutines.flow.Flow
import mihon.core.platform.PlatformContext
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure

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
