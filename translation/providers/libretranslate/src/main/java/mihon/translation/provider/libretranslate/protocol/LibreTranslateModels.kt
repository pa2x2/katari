package mihon.translation.provider.libretranslate.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal data class LibreTranslateLanguage(
    val code: String,
    val name: String,
    val targets: Set<String>,
) {
    init {
        require(code.isNotBlank())
        require(name.isNotBlank())
        require(targets.none(String::isBlank))
    }
}

@Serializable
internal data class LibreTranslateLanguageResponse(
    val code: String,
    val name: String,
    val targets: List<String> = emptyList(),
)

@Serializable
internal data class LibreTranslateRequest(
    val q: String,
    val source: String,
    val target: String,
    val format: String = "text",
    @SerialName("api_key")
    val apiKey: String? = null,
)

@Serializable
internal data class LibreTranslateResponse(
    val translatedText: String,
)

internal enum class LibreTranslateFailureKind {
    Connection,
    Rejected,
    Server,
    InvalidResponse,
}

internal class LibreTranslateException(
    val kind: LibreTranslateFailureKind,
) : Exception(
    when (kind) {
        LibreTranslateFailureKind.Connection -> "LibreTranslate connection failed"
        LibreTranslateFailureKind.Rejected -> "LibreTranslate rejected the request"
        LibreTranslateFailureKind.Server -> "LibreTranslate server failed"
        LibreTranslateFailureKind.InvalidResponse -> "LibreTranslate returned an invalid response"
    },
)
