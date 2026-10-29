package eu.kanade.tachiyomi.ui.entry.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import mihon.core.platform.PlatformContext
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure

@Inject
@ContributesBinding(AppScope::class)
class DefaultEntryTranslateSetupHost(
    private val translationHost: TranslationHostActions,
    private val recognitionHost: TextRecognitionHostActions,
    private val modelStore: ModelArtifactStore,
    private val settingsScreens: EntryTranslateSettingsScreens,
) : EntryTranslateSetupHost {
    override fun recordRecentLanguage(language: LanguageTag) = translationHost.recordRecentLanguage(language)

    override fun downloadModel(approval: ModelArtifactDownloadApproval) = modelStore.download(approval)

    override fun observeModel(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> =
        modelStore.observe(descriptor)

    override suspend fun installPlatformModels(component: TextRecognitionComponentId, language: LanguageTag) {
        recognitionHost.installPlatformModels(component, language)
    }

    override suspend fun acknowledgeDisclosure(engine: TranslationEngineId, disclosure: TranslationProviderDisclosure) {
        translationHost.acknowledgeProviderDisclosure(engine, disclosure)
    }

    override suspend fun downloadTranslationModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
    ) {
        translationHost.downloadModels(engine, models)
    }

    override suspend fun openTranslationSetup(engine: TranslationEngineId) {
        translationHost.openSetup(engine)
    }

    override fun openPipelineChoice(context: PlatformContext, language: LanguageTag) =
        settingsScreens.openPipelineChoice(context, language)

    override fun openTranslationSettings(context: PlatformContext) = settingsScreens.openTranslationSettings(context)
}
