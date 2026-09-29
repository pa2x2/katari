package mihon.text.recognition.provider.mlkit

import android.content.Context
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionScript
import java.util.concurrent.ConcurrentHashMap

/**
 * One ML Kit recognizer per script, created on first use and kept for the life of the process.
 *
 * The app removes ML Kit's startup provider, so the first recognizer also initializes ML Kit.
 */
internal class MlKitScriptRecognizers(
    private val context: Context,
) {
    private val recognizers = ConcurrentHashMap<MlKitTextRecognitionScript, TextRecognizer>()

    operator fun get(script: MlKitTextRecognitionScript): TextRecognizer = recognizers.computeIfAbsent(script) {
        MlKitContext.initializeIfNeeded(context)
        TextRecognition.getClient(
            when (script) {
                MlKitTextRecognitionScript.Latin -> TextRecognizerOptions.DEFAULT_OPTIONS
                MlKitTextRecognitionScript.Japanese -> JapaneseTextRecognizerOptions.Builder().build()
                MlKitTextRecognitionScript.Chinese -> ChineseTextRecognizerOptions.Builder().build()
                MlKitTextRecognitionScript.Korean -> KoreanTextRecognizerOptions.Builder().build()
                MlKitTextRecognitionScript.Devanagari -> DevanagariTextRecognizerOptions.Builder().build()
            },
        )
    }
}
