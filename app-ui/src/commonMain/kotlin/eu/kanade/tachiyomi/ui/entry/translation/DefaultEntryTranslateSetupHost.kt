package eu.kanade.tachiyomi.ui.entry.translation

import app.katari.core.platform.PlatformContext
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.ModelArtifactState
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.host.TranslationHostActions
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.provider.TranslationProviderDisclosure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

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
