package mihon.text.recognition.provider.tesseract

import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense

/**
 * One artifact per language, laid out as Tesseract expects: its files live in a `tessdata` directory whose parent is
 * the data path Tesseract is initialized with.
 */
internal object TesseractModelArtifacts {
    private const val REPOSITORY = "https://github.com/tesseract-ocr/tessdata_fast"
    private const val COMMIT = "87416418657359cb625c412a48b6e1d6d41c29bd"
    private const val FILES = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/$COMMIT"
    const val DATA_DIRECTORY = "tessdata"

    private val artifacts: Map<TesseractLanguage, ModelArtifactDescriptor> =
        TesseractLanguage.entries.associateWith { language ->
            ModelArtifactDescriptor(
                id = ModelArtifactId("tesseract.${language.model.code.replace('_', '-')}"),
                revision = COMMIT,
                displayName = "Tesseract ${language.name}",
                files = listOfNotNull(language.model, language.vertical).map { model ->
                    ModelArtifactFile(
                        name = fileName(model),
                        url = "$FILES/${model.code}.traineddata",
                        sizeBytes = model.sizeBytes,
                        sha256 = model.sha256,
                    )
                },
                license = ModelArtifactLicense("Apache-2.0", REPOSITORY),
                hosting = ModelArtifactHosting.Upstream(REPOSITORY),
            )
        }

    fun artifact(language: TesseractLanguage): ModelArtifactDescriptor = artifacts.getValue(language)

    fun fileName(model: TesseractModelFile): String = "$DATA_DIRECTORY/${model.code}.traineddata"
}
