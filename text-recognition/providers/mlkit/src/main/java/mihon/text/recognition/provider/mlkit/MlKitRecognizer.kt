package mihon.text.recognition.provider.mlkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionCatalog
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionScript
import mihon.text.recognition.spi.component.RecognizedCropText
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognitionPlatformModelsInstallation
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels

/** Reads a detected region with the ML Kit model of the language's script. */
internal class MlKitRecognizer(
    private val recognizers: MlKitScriptRecognizers,
    private val modules: MlKitScriptModules,
) : TextRecognizer {
    override val catalogEntry = MlKitTextRecognitionCatalog.recognizer
    override val inputEdge: Int = INPUT_EDGE

    /** Play services manages ML Kit's models, so none are downloaded through the model store. */
    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> = emptyList()

    override suspend fun inspectDevice(language: LanguageTag): TextRecognitionComponentAvailability {
        val script = MlKitTextRecognitionScript.forLanguage(language)
            ?: return TextRecognitionComponentAvailability.Unavailable("ML Kit cannot read ${language.value}.")
        return modules.availability(script)
    }

    override suspend fun installPlatformModels(language: LanguageTag): TextRecognitionPlatformModelsInstallation {
        val script = MlKitTextRecognitionScript.forLanguage(language)
            ?: return TextRecognitionPlatformModelsInstallation.Failed("ML Kit cannot read ${language.value}.")
        return modules.install(script)
    }

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        val script = MlKitTextRecognitionScript.forLanguage(language) ?: return null
        val scale = (MINIMUM_EDGE.toFloat() / maxOf(crop.width, crop.height)).coerceIn(1f, MAXIMUM_UPSCALE)
        val input = if (scale > 1f) {
            Bitmap.createScaledBitmap(crop, (crop.width * scale).toInt(), (crop.height * scale).toInt(), true)
        } else {
            crop
        }
        val text = try {
            recognizers[script].process(InputImage.fromBitmap(input, 0)).await()
        } finally {
            if (input !== crop) input.recycle()
        }
        val lines = text.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            MlKitLine(line.text, box.left, box.top, box.right, box.bottom)
        }
        return assembleCropText(lines, script.spaced)
    }

    private companion object {
        /** ML Kit needs about 24 px per CJK character and 16 px per Latin one, which crops of this size preserve. */
        const val INPUT_EDGE = 960

        /**
         * Lettering in speech bubbles is often smaller than the 16 to 24 px per character ML Kit needs, so smaller
         * crops are enlarged to about this edge.
         */
        const val MINIMUM_EDGE = 960
        const val MAXIMUM_UPSCALE = 3f
    }
}
