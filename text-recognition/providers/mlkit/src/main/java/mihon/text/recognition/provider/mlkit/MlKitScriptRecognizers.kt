package mihon.text.recognition.provider.mlkit

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionScript
import java.util.concurrent.ConcurrentHashMap

/** One ML Kit recognizer per script, created on first use and kept for the life of the process. */
internal class MlKitScriptRecognizers {
    private val recognizers = ConcurrentHashMap<MlKitTextRecognitionScript, TextRecognizer>()

    operator fun get(script: MlKitTextRecognitionScript): TextRecognizer = recognizers.computeIfAbsent(script) {
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
