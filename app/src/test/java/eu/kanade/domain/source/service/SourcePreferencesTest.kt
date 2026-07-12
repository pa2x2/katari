package eu.kanade.domain.source.service

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class SourcePreferencesTest {

    private val preferences = SourcePreferences(
        preferenceStore = InMemoryPreferenceStore(),
        json = Json,
    )

    @Test
    fun `source preferences use unified source keys`() {
        preferences.disabledSources.set(setOf("1"))
        preferences.pinnedSources.set(setOf("2"))
        preferences.lastUsedSource.set(3L)

        preferences.disabledSources.key() shouldBe SourcePreferences.HIDDEN_SOURCES_KEY
        preferences.disabledSources.get() shouldBe setOf("1")

        preferences.pinnedSources.key() shouldBe SourcePreferences.PINNED_SOURCES_KEY
        preferences.pinnedSources.get() shouldBe setOf("2")

        preferences.lastUsedSource.get() shouldBe 3L
    }
}
