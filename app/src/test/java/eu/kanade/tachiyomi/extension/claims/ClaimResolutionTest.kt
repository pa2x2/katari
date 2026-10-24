package eu.kanade.tachiyomi.extension.claims

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.domain.extension.model.ContentWarning
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.RegisteredSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceProvider
import tachiyomi.domain.source.identity.SourceProviderKind

class ClaimResolutionTest {

    @Test
    fun `an aniyomi source sharing a keiyoushi source's id keeps its own key and leaves the keiyoushi one alone`() {
        // As Keiyoushi's and Yūzōnō's MyReadingManga; the Aniyomi kind wrapped its source with the key it was given
        val keiyoushiSource = source(id = 6318378620301107421)
        val aniyomiSource = source(id = -12)
        val keiyoushi =
            extension(
                "eu.kanade.tachiyomi.extension.en.myreadingmanga",
                SourceIdScheme.TACHIYOMI,
                SourceProviderKind.TACHIYOMI_EXTENSION,
                keiyoushiSource,
            )
        val aniyomi =
            extension(
                "eu.kanade.tachiyomi.animeextension.all.myreadingmanga",
                SourceIdScheme.ANIYOMI,
                SourceProviderKind("aniyomi-extension"),
                aniyomiSource,
            )
        val registered = listOf(
            RegisteredSource(6318378620301107421, SourceIdentity(SourceIdScheme.TACHIYOMI, 6318378620301107421), null),
            RegisteredSource(-12, SourceIdentity(SourceIdScheme.ANIYOMI, 6318378620301107421), null),
        )
        val extensions = listOf(keiyoushi, aniyomi)

        identitiesToRegister(extensions, registered).shouldBeEmpty()
        val resolution = resolveClaims(extensions, registered)

        resolution.conflicted.shouldBeEmpty()
        resolution.loaded.getValue(keiyoushi.pkgName).sources shouldContainExactly listOf(keiyoushiSource)
        resolution.loaded.getValue(aniyomi.pkgName).sources shouldContainExactly listOf(aniyomiSource)
        resolution.newProviders shouldBe mapOf(
            6318378620301107421 to SourceProvider(keiyoushi.providerKind, keiyoushi.pkgName),
            -12L to SourceProvider(aniyomi.providerKind, aniyomi.pkgName),
        )
    }

    private fun source(id: Long) = mockk<UnifiedSource> {
        every { this@mockk.id } returns id
        every { name } returns "MyReadingManga"
    }

    private fun extension(
        pkgName: String,
        scheme: SourceIdScheme,
        providerKind: SourceProviderKind,
        source: UnifiedSource,
    ) = Extension.Loaded(
        name = "MyReadingManga",
        pkgName = pkgName,
        versionName = "1.0",
        versionCode = 1,
        libVersion = 1.6,
        lang = "en",
        contentWarning = ContentWarning.NSFW,
        pkgFactory = null,
        sources = listOf(source),
        sourceIdScheme = scheme,
        providerKind = providerKind,
        isShared = true,
        signatures = emptyList(),
    )
}
