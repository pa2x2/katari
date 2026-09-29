package mihon.text.recognition.provider.onnx.catalog

import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense
import mihon.text.recognition.provider.onnx.paddle.PaddleOcrScript

/**
 * Upstream model publications pinned to exact commits. Sizes and digests come from the Hugging Face LFS metadata of
 * those commits; a different file can never be installed under these revisions.
 */
internal object OnnxModelArtifacts {
    private const val DETECTOR_REPOSITORY = "https://huggingface.co/ogkalu/comic-text-and-bubble-detector"
    private const val DETECTOR_COMMIT = "16e8a622f91fabc6b5b65c96d32d1183f8843546"
    private const val MANGA_OCR_ONNX_REPOSITORY = "https://huggingface.co/onnx-community/manga-ocr-base-ONNX"
    private const val MANGA_OCR_ONNX_COMMIT = "f9023406bb2f6b17df67bc4a327c56ecd20611f0"
    private const val MANGA_OCR_REPOSITORY = "https://huggingface.co/kha-white/manga-ocr-base"
    private const val MANGA_OCR_COMMIT = "aa6573bd10b0d446cbf622e29c3e084914df9741"

    private const val PADDLE_OCR_REPOSITORY = "https://huggingface.co/monkt/paddleocr-onnx"
    private const val PADDLE_OCR_COMMIT = "7b02d0a30a07ba2b92ad1ff5a8941ae2c633de65"

    const val DETECTOR_FILE = "detector-v4-s_int8.onnx"
    const val PADDLE_OCR_LINE_DETECTOR_FILE = "det.onnx"
    const val PADDLE_OCR_RECOGNIZER_FILE = "rec.onnx"
    const val PADDLE_OCR_DICTIONARY_FILE = "dict.txt"
    const val MANGA_OCR_ENCODER_FILE = "encoder_model_int8.onnx"
    const val MANGA_OCR_DECODER_FILE = "decoder_model_int8.onnx"
    const val MANGA_OCR_VOCABULARY_FILE = "vocab.txt"

    val comicTextDetector = ModelArtifactDescriptor(
        id = ModelArtifactId("onnx.comic-text-and-bubble-detector"),
        revision = DETECTOR_COMMIT,
        displayName = "Comic text and bubble detector",
        files = listOf(
            ModelArtifactFile(
                name = DETECTOR_FILE,
                url = "$DETECTOR_REPOSITORY/resolve/$DETECTOR_COMMIT/$DETECTOR_FILE",
                sizeBytes = 11_120_765,
                sha256 = "5fe9e4f576e49d4e7e8b0e029d6d3cdc252abd4694113e1cae120e62c931ea79",
            ),
        ),
        license = ModelArtifactLicense("Apache-2.0", DETECTOR_REPOSITORY),
        hosting = ModelArtifactHosting.Upstream(DETECTOR_REPOSITORY),
    )

    val mangaOcr = ModelArtifactDescriptor(
        id = ModelArtifactId("onnx.manga-ocr"),
        revision = "${MANGA_OCR_ONNX_COMMIT.take(12)}-${MANGA_OCR_COMMIT.take(12)}",
        displayName = "Manga OCR",
        files = listOf(
            ModelArtifactFile(
                name = MANGA_OCR_ENCODER_FILE,
                url = "$MANGA_OCR_ONNX_REPOSITORY/resolve/$MANGA_OCR_ONNX_COMMIT/onnx/$MANGA_OCR_ENCODER_FILE",
                sizeBytes = 86_967_767,
                sha256 = "ddd1af56963093795705fa38da6ce7e6567d1658e7c7359db7e13fcd37dbf279",
            ),
            ModelArtifactFile(
                name = MANGA_OCR_DECODER_FILE,
                url = "$MANGA_OCR_ONNX_REPOSITORY/resolve/$MANGA_OCR_ONNX_COMMIT/onnx/$MANGA_OCR_DECODER_FILE",
                sizeBytes = 29_627_936,
                sha256 = "2e7177d2b0a59f1c612b694ed70c13971bee765cc2b2bc7bc9376e4753652f27",
            ),
            ModelArtifactFile(
                name = MANGA_OCR_VOCABULARY_FILE,
                url = "$MANGA_OCR_REPOSITORY/resolve/$MANGA_OCR_COMMIT/$MANGA_OCR_VOCABULARY_FILE",
                sizeBytes = 24_072,
                sha256 = "344fbb6b8bf18c57839e924e2c9365434697e0227fac00b88bb4899b78aa594d",
            ),
        ),
        license = ModelArtifactLicense("Apache-2.0", MANGA_OCR_REPOSITORY),
        hosting = ModelArtifactHosting.Upstream(MANGA_OCR_ONNX_REPOSITORY),
    )

    /**
     * PP-OCRv3 mobile text detector, used to split a text region into lines. It stays at v3 although most recognizers
     * are v5: on speech bubbles the v5 mobile detector reads no better and also picks up furigana and artwork, and the
     * v5 server detector is 36 times larger and 25 times slower.
     */
    val paddleOcrLineDetector = ModelArtifactDescriptor(
        id = ModelArtifactId("onnx.paddleocr-line-detector"),
        revision = PADDLE_OCR_COMMIT,
        displayName = "PaddleOCR line detector",
        files = listOf(
            ModelArtifactFile(
                name = PADDLE_OCR_LINE_DETECTOR_FILE,
                url = "$PADDLE_OCR_REPOSITORY/resolve/$PADDLE_OCR_COMMIT/detection/v3/$PADDLE_OCR_LINE_DETECTOR_FILE",
                sizeBytes = 2_429_873,
                sha256 = "ee40e80071ba3a320d4efda75f3e22047a7d049e9bf7bcaaf9daea23fc21b935",
            ),
        ),
        license = ModelArtifactLicense("Apache-2.0", PADDLE_OCR_REPOSITORY),
        hosting = ModelArtifactHosting.Upstream(PADDLE_OCR_REPOSITORY),
    )

    private val paddleOcrRecognizers: Map<PaddleOcrScript, ModelArtifactDescriptor> =
        PaddleOcrScript.entries.associateWith { script ->
            val folder = "$PADDLE_OCR_REPOSITORY/resolve/$PADDLE_OCR_COMMIT/languages/${script.folder}"
            ModelArtifactDescriptor(
                id = ModelArtifactId("onnx.paddleocr-${script.folder}"),
                revision = PADDLE_OCR_COMMIT,
                displayName = "PaddleOCR ${script.displayName}",
                files = listOf(
                    ModelArtifactFile(
                        name = PADDLE_OCR_RECOGNIZER_FILE,
                        url = "$folder/$PADDLE_OCR_RECOGNIZER_FILE",
                        sizeBytes = script.recognizerSize,
                        sha256 = script.recognizerSha256,
                    ),
                    ModelArtifactFile(
                        name = PADDLE_OCR_DICTIONARY_FILE,
                        url = "$folder/$PADDLE_OCR_DICTIONARY_FILE",
                        sizeBytes = script.dictionarySize,
                        sha256 = script.dictionarySha256,
                    ),
                ),
                license = ModelArtifactLicense("Apache-2.0", PADDLE_OCR_REPOSITORY),
                hosting = ModelArtifactHosting.Upstream(PADDLE_OCR_REPOSITORY),
            )
        }

    fun paddleOcrRecognizer(script: PaddleOcrScript): ModelArtifactDescriptor = paddleOcrRecognizers.getValue(script)
}
