package mihon.translation.provider.deepl.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A language as the server names it, such as `JA` or `EN-US`. */
internal data class DeepLLanguage(
    val code: String,
    val name: String,
) {
    init {
        require(code.isNotBlank())
    }
}

/** The languages a server translates from and into; it translates from any of the former into any of the latter. */
internal data class DeepLLanguages(
    val sources: List<DeepLLanguage>,
    val targets: List<DeepLLanguage>,
)

@Serializable
internal data class DeepLLanguageResponse(
    val language: String,
    val name: String = "",
)

@Serializable
internal data class DeepLTranslateRequest(
    val text: List<String>,
    @SerialName("source_lang")
    val sourceLanguage: String,
    @SerialName("target_lang")
    val targetLanguage: String,
    val context: String? = null,
)

@Serializable
internal data class DeepLTranslateResponse(
    val translations: List<DeepLTranslation>,
)

@Serializable
internal data class DeepLTranslation(
    val text: String,
)
