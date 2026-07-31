package mihon.translation.api

import io.kotest.assertions.throwables.shouldThrow
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineDetails
import org.junit.jupiter.api.Test

class TranslationEnginePresentationTest {
    @Test
    fun `bundled artwork requires a real resource`() {
        shouldThrow<IllegalArgumentException> {
            TranslationEngineArtwork.Bundled(resourceId = 0)
        }
    }

    @Test
    fun `installed application artwork requires package and provider fallback`() {
        shouldThrow<IllegalArgumentException> {
            TranslationEngineArtwork.InstalledApplication(
                packageName = " ",
                fallbackResourceId = 1,
            )
        }
        shouldThrow<IllegalArgumentException> {
            TranslationEngineArtwork.InstalledApplication(
                packageName = "example.translator",
                fallbackResourceId = 0,
            )
        }
    }

    @Test
    fun `engine details require every privacy field`() {
        listOf(
            Triple("", "On this device", "Private"),
            Triple("Description", "", "Private"),
            Triple("Description", "On this device", ""),
        ).forEach { (description, processing, privacy) ->
            shouldThrow<IllegalArgumentException> {
                TranslationEngineDetails(
                    description = description,
                    processingLocation = processing,
                    privacyDescription = privacy,
                )
            }
        }
    }

    @Test
    fun `artwork attribution URL requires attribution text`() {
        shouldThrow<IllegalArgumentException> {
            TranslationEngineDetails(
                description = "Description",
                processingLocation = "On this device",
                privacyDescription = "Private",
                artworkAttributionUrl = "https://example.org",
            )
        }
    }
}
