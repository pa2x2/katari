package eu.kanade.tachiyomi.extension.claims

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import mihon.domain.extension.model.ContentWarning
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

class UnclaimedSourcesTest {

    @Test
    fun `a reload lets go only of the instances an update replaced, not the ones it reused`() {
        val reused = source(id = -1)
        val replaced = source(id = -2)
        val replacement = source(id = -2)
        val unchanged = extension("unchanged", reused)
        val updated = extension("updated", replaced)

        unclaimedSources(
            before = listOf(unchanged, updated),
            after = listOf(
                unchanged.copy(hasUpdate = true),
                updated.copy(versionCode = 2, sources = listOf(replacement)),
            ),
        ) shouldContainExactly listOf(replaced)
    }

    private fun source(id: Long) = mockk<UnifiedSource> {
        every { this@mockk.id } returns id
    }

    private fun extension(pkgName: String, vararg sources: UnifiedSource) = Extension.Loaded(
        name = pkgName,
        pkgName = pkgName,
        versionName = "14.1",
        versionCode = 1,
        libVersion = 14.0,
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        pkgFactory = null,
        sources = sources.toList(),
        sourceIdScheme = SourceIdScheme.ANIYOMI,
        providerKind = SourceProviderKind("aniyomi-extension"),
        isShared = true,
        signatures = emptyList(),
    )
}
