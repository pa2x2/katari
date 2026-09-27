package mihon.text.recognition.provider.tesseract

import mihon.language.api.tag.LanguageTag

/**
 * Tesseract `tessdata_fast` models, pinned by commit, per supported language. Sizes and digests are those of the
 * pinned commit.
 *
 * @property vertical the model for vertical text, for scripts that print it.
 * @property spaced whether words are separated by spaces; recognized text of unspaced scripts drops them.
 */
internal enum class TesseractLanguage(
    languageCode: String,
    val model: TesseractModelFile,
    val vertical: TesseractModelFile? = null,
    val spaced: Boolean = true,
) {
    English(
        "en",
        TesseractModelFile("eng", 4_113_088, "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2"),
    ),
    Japanese(
        languageCode = "ja",
        model = TesseractModelFile(
            "jpn",
            2_471_260,
            "1f5de9236d2e85f5fdf4b3c500f2d4926f8d9449f28f5394472d9e8d83b91b4d",
        ),
        vertical = TesseractModelFile(
            "jpn_vert",
            3_037_480,
            "bf1e2640954691797e2dc14f38533e601b59ee37958698ae0f0b81dc6f09c71b",
        ),
        spaced = false,
    ),
    Korean(
        "ko",
        TesseractModelFile("kor", 1_677_415, "6b85e11d9bbf07863b97b3523b1b112844c43e713df8b66418a081fd1060b3b2"),
    ),
    Chinese(
        languageCode = "zh",
        model = TesseractModelFile(
            "chi_sim",
            2_469_156,
            "a5fcb6f0db1e1d6d8522f39db4e848f05984669172e584e8d76b6b3141e1f730",
        ),
        spaced = false,
    ),
    French(
        "fr",
        TesseractModelFile("fra", 1_130_365, "ced037562e8c80c13122dece28dd477d399af80911a28791a66a63ac1e3445ca"),
    ),
    German(
        "de",
        TesseractModelFile("deu", 1_525_436, "19d219bbb6672c869d20a9636c6816a81eb9a71796cb93ebe0cb1530e2cdb22d"),
    ),
    Spanish(
        "es",
        TesseractModelFile("spa", 2_294_433, "6f2e04d02774a18f01bed44b1111f2cd7f3ba7ac9dc4373cd3f898a40ea6b464"),
    ),
    Italian(
        "it",
        TesseractModelFile("ita", 2_701_314, "b8f89e1e785118dac4d51ae042c029a64edb5c3ee42ef73027a6d412748d8827"),
    ),
    Portuguese(
        "pt",
        TesseractModelFile("por", 1_982_756, "c4932b937207a9514b7514d518b931a99938c02a28a5a5a553f8599ed58b7deb"),
    ),
    Russian(
        "ru",
        TesseractModelFile("rus", 3_861_738, "e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225"),
    ),
    Ukrainian(
        "uk",
        TesseractModelFile("ukr", 3_825_102, "d59e53e2bded32f4445f124b4b00240fcac7e8044c003ab822ccb94f0b3db59b"),
    ),
    Polish(
        "pl",
        TesseractModelFile("pol", 4_765_518, "c4476cdbc0e33d898d32345122b7be1cbf85ace15f920f06c7714756e1ef79b2"),
    ),
    Dutch(
        "nl",
        TesseractModelFile("nld", 6_050_296, "ced0e5e046a84c908a6aa7accbef9a232c4a5d9a8276691b81c6ee64d02963f6"),
    ),
    Turkish(
        "tr",
        TesseractModelFile("tur", 4_550_554, "7393381111e1152420fc4092cb44eef4237580d21b92bf30d7d221aad192c6b7"),
    ),
    Vietnamese(
        "vi",
        TesseractModelFile("vie", 531_275, "79df64caf7bcfb2a27df5042ecb6121e196eada34da774956995747636d5bfa1"),
    ),
    Indonesian(
        "id",
        TesseractModelFile("ind", 1_122_661, "69786901da87ab8766c1ea7fbb10b28f2110c14da3f6c8f2735df131fba95d88"),
    ),
    Thai(
        languageCode = "th",
        model = TesseractModelFile(
            "tha",
            1_072_600,
            "294227cc2d1292b0acb28d61d4115c88252b96d466ca90b417cf4cf0c67bf07c",
        ),
        spaced = false,
    ),
    Arabic(
        "ar",
        TesseractModelFile("ara", 1_432_056, "e3206d3dc87fd50c24a0fb9f01838615911d25168f4e64415244b67d2bb3e729"),
    ),
    ;

    val tag: LanguageTag = LanguageTag.require(languageCode)

    companion object {
        fun forLanguage(language: LanguageTag): TesseractLanguage? {
            val primary = language.value.substringBefore('-')
            return entries.firstOrNull { it.tag.value == primary }
        }
    }
}

/** One `<code>.traineddata` file of the pinned `tessdata_fast` commit. */
internal data class TesseractModelFile(
    val code: String,
    val sizeBytes: Long,
    val sha256: String,
)
