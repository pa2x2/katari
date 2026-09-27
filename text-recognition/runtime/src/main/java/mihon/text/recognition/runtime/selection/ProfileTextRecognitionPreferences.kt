package mihon.text.recognition.runtime.selection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.runtime.language.recognitionLanguage
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern

/**
 * One profile's recognition configuration: its engine, and per-language overrides indexed by an explicit language set
 * so they can be listed without scanning the preference store.
 */
internal class ProfileTextRecognitionPreferences(
    private val preferenceStore: PreferenceStore,
) {
    private val selections = mutableMapOf<String, Preference<TextRecognitionPipelineSelection?>>()
    private val provider: Preference<String> = preferenceStore.getString(PROVIDER_KEY, "")
    private val overrideLanguages: Preference<Set<String>> = preferenceStore.getStringSet(
        OVERRIDE_LANGUAGES_KEY,
        emptySet(),
    )

    fun configuration(): TextRecognitionConfiguration = configuration(provider.get(), overrideLanguages.get())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeConfiguration(): Flow<TextRecognitionConfiguration> =
        combine(provider.changes(), overrideLanguages.changes(), ::Pair).flatMapLatest { (provider, languages) ->
            if (languages.isEmpty()) {
                flowOf(configuration(provider, languages))
            } else {
                combine(languages.map { selection(it).changes() }) { configuration(provider, languages) }
            }
        }

    fun save(configuration: TextRecognitionConfiguration) {
        val overrides = configuration.overrides.mapKeys { (language, _) -> language.recognitionLanguage }
        (overrideLanguages.get() - overrides.keys).forEach { selection(it).delete() }
        overrides.forEach { (language, selection) -> selection(language).set(selection) }
        overrideLanguages.set(overrides.keys)
        configuration.provider?.let { provider.set(it.value) } ?: provider.delete()
    }

    private fun configuration(provider: String, languages: Set<String>) = TextRecognitionConfiguration(
        provider = provider.takeIf(String::isNotEmpty)?.let {
            runCatching { TextRecognitionProviderId(it) }.getOrNull()
        },
        overrides = languages.mapNotNull { language ->
            val tag = LanguageTag.parse(language) ?: return@mapNotNull null
            selection(language).get()?.let { tag to it }
        }.toMap(),
    )

    private fun selection(language: String): Preference<TextRecognitionPipelineSelection?> =
        synchronized(selections) {
            selections.getOrPut(language) {
                preferenceStore.getObjectFromString(
                    key = SELECTION_KEY_FAMILY.key(language),
                    defaultValue = null,
                    serializer = TextRecognitionSelectionCodec::encode,
                    deserializer = TextRecognitionSelectionCodec::decode,
                )
            }
        }

    companion object {
        val SELECTION_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix("text_recognition_pipeline_")
        private const val PROVIDER_KEY = "text_recognition_provider"
        private const val OVERRIDE_LANGUAGES_KEY = "text_recognition_override_languages"
    }
}
