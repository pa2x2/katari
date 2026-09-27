package mihon.text.recognition.runtime.selection

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.runtime.language.recognitionLanguage
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern

/** Per-language pipeline choices of one profile. An absent choice means the build's default preset applies. */
internal class ProfileTextRecognitionPreferences(
    private val preferenceStore: PreferenceStore,
) {
    private val selections = mutableMapOf<String, Preference<TextRecognitionPipelineSelection?>>()

    fun selection(language: LanguageTag): Preference<TextRecognitionPipelineSelection?> {
        val key = language.recognitionLanguage
        return synchronized(selections) {
            selections.getOrPut(key) {
                preferenceStore.getObjectFromString(
                    key = SELECTION_KEY_FAMILY.key(key),
                    defaultValue = null,
                    serializer = TextRecognitionSelectionCodec::encode,
                    deserializer = TextRecognitionSelectionCodec::decode,
                )
            }
        }
    }

    companion object {
        val SELECTION_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix("text_recognition_pipeline_")
    }
}
