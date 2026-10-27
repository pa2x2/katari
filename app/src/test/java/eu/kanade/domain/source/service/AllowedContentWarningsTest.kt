package eu.kanade.domain.source.service

import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ContentWarning
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference

class AllowedContentWarningsTest {

    // A profile that chose its warnings before Not rated existed hid every LNReader plugin after the update
    @Test
    fun `a choice saved before Not rated existed allows it, until the profile turns it off`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreference(
                    "enabled_content_warnings",
                    setOf(ContentWarning.SAFE, ContentWarning.MIXED),
                    emptySet<ContentWarning>(),
                ),
            ),
        )
        val allowed = SourcePreferences.enabledContentWarnings(store)

        allowed.get() shouldBe setOf(ContentWarning.SAFE, ContentWarning.MIXED, ContentWarning.NOT_RATED)

        allowed.set(setOf(ContentWarning.SAFE))
        allowed.get() shouldBe setOf(ContentWarning.SAFE)
    }
}
