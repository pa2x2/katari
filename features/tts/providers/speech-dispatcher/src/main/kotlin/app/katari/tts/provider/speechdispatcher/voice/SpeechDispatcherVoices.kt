package app.katari.tts.provider.speechdispatcher.voice

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsProviderId
import app.katari.tts.api.provider.TtsVoiceProcessing
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId

/** A voice as Speech Dispatcher names it: [language] is in its own spelling, which `SET self LANGUAGE` takes back. */
internal data class SpeechDispatcherVoice(
    val module: String,
    val name: String,
    val language: String,
)

/**
 * The voices one module lists (`name TAB language TAB variant`), without the variants. A variant is a timbre a
 * module lays over every base voice — espeak-ng lists each of its hundred-odd languages with each of its hundred-odd
 * variants — so only base voices, whose variant is `none`, are offered when the module has them.
 */
internal fun parseSynthesisVoices(module: String, lines: List<String>): List<SpeechDispatcherVoice> {
    val rows = lines.mapNotNull { line ->
        val fields = line.split('\t')
        val name = fields.getOrNull(0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val language = fields.getOrNull(1)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        VoiceRow(SpeechDispatcherVoice(module, name, language), variant = fields.getOrNull(2))
    }
    val baseRows = rows.filter { it.variant == NO_VARIANT }
    return baseRows.ifEmpty { rows }.map(VoiceRow::voice)
}

/** The voice's id holds its module and name, which together name it in Speech Dispatcher. */
internal fun speechDispatcherVoiceId(
    provider: TtsProviderId,
    engine: TtsEngineId,
    voice: SpeechDispatcherVoice,
): TtsVoiceId = TtsVoiceId(provider, engine, "${voice.module}$ID_SEPARATOR${voice.name}")

/**
 * The voice for Katari's settings, or null for a language Katari can't name. With several modules installed, a voice
 * names its module, as two modules can each have a voice for the same language under a similar name.
 */
internal fun SpeechDispatcherVoice.toApiVoice(
    provider: TtsProviderId,
    engine: TtsEngineId,
    namesModule: Boolean,
): TtsVoice? {
    val tag = LanguageTag.parse(language.withMacrolanguage()) ?: return null
    return TtsVoice(
        id = speechDispatcherVoiceId(provider, engine, this),
        name = if (namesModule) "$name · $module" else name,
        language = tag,
        // Speech Dispatcher's synthesizers run on the computer it runs on.
        processing = TtsVoiceProcessing.OnDevice,
    )
}

/**
 * espeak-ng tags Mandarin with its individual-language code `cmn`; text and books tag it `zh`, the macrolanguage, which
 * is what matches them to a voice.
 */
private fun String.withMacrolanguage(): String {
    val primary = substringBefore('-')
    val replacement = MACROLANGUAGES[primary.lowercase()] ?: return this
    return replacement + removePrefix(primary)
}

private data class VoiceRow(val voice: SpeechDispatcherVoice, val variant: String?)

private const val NO_VARIANT = "none"
private const val ID_SEPARATOR = ':'
private val MACROLANGUAGES = mapOf("cmn" to "zh")
