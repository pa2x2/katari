package mihon.text.recognition.provider.onnx.paddle

import mihon.language.api.tag.LanguageTag

/**
 * Recognition models, one per script group, as published in `monkt/paddleocr-onnx`: PP-OCRv5 where PaddleOCR has one
 * (the Chinese model is the server variant, the others mobile), and PP-OCRv3 mobile for Arabic, Devanagari, Tamil, and
 * Telugu. Sizes and digests are those of the pinned commit.
 *
 * @property spaced whether words are separated by spaces, so lines of one region are joined with a space.
 * @property vertical whether the script is also printed in vertical columns, which are read turned on their side.
 */
internal enum class PaddleOcrScript(
    val folder: String,
    val displayName: String,
    languageCodes: List<String>,
    val recognizerSize: Long,
    val recognizerSha256: String,
    val dictionarySize: Long,
    val dictionarySha256: String,
    val spaced: Boolean = true,
    val vertical: Boolean = false,
) {
    English(
        folder = "english",
        displayName = "English",
        languageCodes = listOf("en"),
        recognizerSize = 7_830_888,
        recognizerSha256 = "4e16deb22c4da6468bdca539b2cd3c8687825538b67109177c47d359ab994cd7",
        dictionarySize = 1_416,
        dictionarySha256 = "e025a66d31f327ba0c232e03f407ae8d105e1e709e7ccb3f408aa778c24e70d6",
    ),
    Latin(
        folder = "latin",
        displayName = "Latin script",
        languageCodes = listOf(
            "fr", "de", "af", "it", "es", "bs", "pt", "cs", "cy", "da", "et", "ga", "hr", "uz", "hu", "id", "oc", "is",
            "lt", "mi", "ms", "nl", "no", "pl", "sk", "sl", "sq", "sv", "sw", "tl", "tr", "la",
        ),
        recognizerSize = 7_862_832,
        recognizerSha256 = "614ffc2d6d3902d360fad7f1b0dd455ee45e877069d14c4e51a99dc4ef144409",
        dictionarySize = 1_634,
        dictionarySha256 = "3c0a8a79b612653c25f765271714f71281e4e955962c153e272b7b8c1d2b13ff",
    ),
    Cyrillic(
        folder = "eslav",
        displayName = "Cyrillic",
        languageCodes = listOf("ru", "uk", "be", "bg"),
        recognizerSize = 7_870_092,
        recognizerSha256 = "dc6bf0e855247decce214ba6dae5bc135fa0ad725a5918a7fcfb59fad6c9cdee",
        dictionarySize = 1_663,
        dictionarySha256 = "3e95f1581557162870cacdba5af91a4c6be2890710d395b0c3c7578e7ee5e6eb",
    ),
    Korean(
        folder = "korean",
        displayName = "Korean",
        languageCodes = listOf("ko"),
        recognizerSize = 13_401_252,
        recognizerSha256 = "322f140154c820fcb83c3d24cfe42c9ec70dd1a1834163306a7338136e4f1eaa",
        dictionarySize = 47_451,
        dictionarySha256 = "a88071c68c01707489baa79ebe0405b7beb5cca229f4fc94cc3ef992328802d7",
    ),
    Chinese(
        folder = "chinese",
        displayName = "Chinese and Japanese",
        languageCodes = listOf("zh", "ja"),
        recognizerSize = 84_468_836,
        recognizerSha256 = "26fa4f47060f58e25962b9af6beaee05c8182b90e026c4ecc6db165d9dfdc38a",
        dictionarySize = 74_012,
        dictionarySha256 = "d1979e9f794c464c0d2e0b70a7fe14dd978e9dc644c0e71f14158cdf8342af1b",
        spaced = false,
        vertical = true,
    ),
    Arabic(
        folder = "arabic",
        displayName = "Arabic script",
        languageCodes = listOf("ar", "fa", "ur"),
        recognizerSize = 8_978_664,
        recognizerSha256 = "7982d371612785238fd99080cff36354deaec84fdc6ff7da9c82af4243fa0c9a",
        dictionarySize = 405,
        dictionarySha256 = "637c27c88512c22089bef927b34ada08f748dc132ac70facd68d8202384c2726",
    ),
    Devanagari(
        folder = "hindi",
        displayName = "Devanagari",
        languageCodes = listOf("hi", "mr", "ne", "sa"),
        recognizerSize = 8_980_224,
        recognizerSha256 = "43df175fa3c877fbf7bcc4e5bd1e203e24ec450cd3ea96c9e802c86e39a4d4cf",
        dictionarySize = 508,
        dictionarySha256 = "b5f1be6d8bbff1a19fb96c5d4ca96a423380234bb7d2ce0e07b5838adb4d18ea",
    ),
    Thai(
        folder = "thai",
        displayName = "Thai",
        languageCodes = listOf("th"),
        recognizerSize = 7_873_480,
        recognizerSha256 = "2b6e56b1872200349e227574c25aeb0e0f9af9b8356e9ff5f75ac543a535669a",
        dictionarySize = 1_767,
        dictionarySha256 = "57f5406f94bb6688fb7077f7be65f08bbd71cecf48c01ea26c522cb5c4836b7a",
        spaced = false,
    ),
    Greek(
        folder = "greek",
        displayName = "Greek",
        languageCodes = listOf("el"),
        recognizerSize = 7_791_200,
        recognizerSha256 = "13373f736dbb229e96945fc41c2573403d91503b0775c7b7294839e0c5f3a7a3",
        dictionarySize = 1_103,
        dictionarySha256 = "31defc62c0c3ad3674a82da6192226a2ba98ef4ff014a7045cb88d59f9c3de31",
    ),
    Tamil(
        folder = "tamil",
        displayName = "Tamil",
        languageCodes = listOf("ta"),
        recognizerSize = 8_970_084,
        recognizerSha256 = "fba9a00af746c8f3ef4f091cf966880222cd7245a60f6a953670593630c0ca4f",
        dictionarySize = 352,
        dictionarySha256 = "e93e694814afd9ff1e918b6f4bb4267fb4c65a9344ee5d7464f9135b90c0a270",
    ),
    Telugu(
        folder = "telugu",
        displayName = "Telugu",
        languageCodes = listOf("te"),
        recognizerSize = 8_976_064,
        recognizerSha256 = "669946d9ad4de93e8d1b13f7e72c59cc2cc2de91bb24e22aad7503a6cc5757ee",
        dictionarySize = 429,
        dictionarySha256 = "ee9946a60d7701977474e89883694a4ca20eaa3868a23334bf7940dcd008f0f9",
    ),
    ;

    val languages: Set<LanguageTag> = languageCodes.map(LanguageTag::require).toSet()

    companion object {
        fun forLanguage(language: LanguageTag): PaddleOcrScript? {
            val primary = language.value.substringBefore('-')
            return entries.firstOrNull { script -> script.languages.any { it.value.substringBefore('-') == primary } }
        }
    }
}
