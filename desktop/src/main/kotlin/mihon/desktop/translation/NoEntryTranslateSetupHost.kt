package mihon.desktop.translation

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
 * Stands in for the translation engines, text recognition, model downloads and settings screens the translation setup
 * sheet's fixes act on, none of which desktop has yet. Nothing reaches it: the sheet only opens for chapters a type
 * translates, and [NoEntryTranslateFeature] translates none. A host backed by desktop's own translation and text
 * recognition services replaces this once desktop has them.
 */
@Inject
class NoEntryTranslateSetupHost : EntryTranslateSetupHost {
    override fun recordRecentLanguage(language: LanguageTag) = Unit

    override fun downloadModel(approval: ModelArtifactDownloadApproval) = Unit

    override fun observeModel(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> =
        flowOf(ModelArtifactState.NotInstalled)

    override suspend fun installPlatformModels(component: TextRecognitionComponentId, language: LanguageTag) = Unit

    override suspend fun acknowledgeDisclosure(engine: TranslationEngineId, disclosure: TranslationProviderDisclosure) =
        Unit

    override suspend fun downloadTranslationModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
    ) = Unit

    override suspend fun openTranslationSetup(engine: TranslationEngineId) = Unit

    override fun openPipelineChoice(context: PlatformContext, language: LanguageTag) = Unit

    override fun openTranslationSettings(context: PlatformContext) = Unit
}
