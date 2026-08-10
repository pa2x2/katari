package mihon.tts.ui.settings

import mihon.language.api.tag.LanguageTag
import java.util.Locale

data class TtsPreviewSample(
    val language: LanguageTag,
    val text: String,
)

fun previewSample(language: LanguageTag): TtsPreviewSample {
    val locale = Locale.forLanguageTag(language.value)
    val text = PREVIEW_SAMPLES[locale.language]
        ?: locale.getDisplayLanguage(locale).ifBlank { language.value }
    return TtsPreviewSample(language, text)
}

private val PREVIEW_SAMPLES = mapOf(
    "de" to "Dies ist eine Vorschau der Sprachausgabe.",
    "en" to "This is a text-to-speech preview.",
    "es" to "Esta es una prueba de texto a voz.",
    "fr" to "Ceci est un aperçu de la synthèse vocale.",
    "it" to "Questa è un'anteprima della sintesi vocale.",
    "ja" to "これは音声読み上げのプレビューです。",
    "ko" to "텍스트 음성 변환 미리 듣기입니다.",
    "pl" to "To jest podgląd syntezy mowy.",
    "pt" to "Esta é uma prévia da conversão de texto em fala.",
    "ru" to "Это предварительное прослушивание синтеза речи.",
    "uk" to "Це попереднє прослуховування синтезу мовлення.",
    "zh" to "这是文字转语音预览。",
)
