package eu.kanade.presentation.more.settings.screen.translation

import eu.kanade.presentation.more.settings.screen.translation.engine.TranslationEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerTarget
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class TranslationPickerScreenSerializationTest {

    @Test
    fun `picker routes remain serializable for navigator state saving`() {
        val routes = listOf(
            TranslationEnginePickerScreen(),
            TranslationLanguagePickerScreen(TranslationLanguagePickerTarget.PlaygroundSource),
            TranslationLanguagePickerScreen(TranslationLanguagePickerTarget.PlaygroundTarget),
        )

        routes.forEach { route ->
            ObjectOutputStream(ByteArrayOutputStream()).use { output ->
                output.writeObject(route)
            }
        }
    }
}
