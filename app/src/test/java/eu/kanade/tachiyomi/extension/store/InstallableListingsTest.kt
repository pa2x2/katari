package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.collections.shouldContainExactly
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme

class InstallableListingsTest {

    @Test
    fun `only listings built with a library version this app runs can be installed, whatever their patch`() {
        val extensions = listOf(
            availableExtension(name = "Legacy 1.4", libVersionName = "1.4"),
            availableExtension(name = "Legacy 1.6", libVersionName = "1.6"),
            availableExtension(name = "Legacy 1.6 patch", libVersionName = "1.6.1"),
            availableExtension(name = "Old fork 1.10", libVersionName = "1.10.0"),
            availableExtension(name = "Entry 2.6", libVersionName = "2.6.0"),
            availableExtension(name = "Entry 2.7", libVersionName = "2.7.0"),
            availableExtension(name = "Entry 3.0", libVersionName = "3.0.0"),
            availableExtension(name = "Entry 3.0 patch", libVersionName = "3.0.1"),
            availableExtension(name = "Future Entry 3.1", libVersionName = "3.1.0"),
            availableExtension(name = "Aniyomi 14", libVersionName = "14", sourceIdScheme = SourceIdScheme.ANIYOMI),
            availableExtension(
                name = "Future Aniyomi 18",
                libVersionName = "18",
                sourceIdScheme = SourceIdScheme.ANIYOMI,
            ),
        )

        extensions
            .filter { it.isInstallable }
            .map { it.name } shouldContainExactly listOf(
            "Legacy 1.6",
            "Legacy 1.6 patch",
            "Entry 3.0",
            "Entry 3.0 patch",
            "Aniyomi 14",
        )
    }

    private fun availableExtension(
        name: String,
        libVersionName: String,
        sourceIdScheme: SourceIdScheme = SourceIdScheme.TACHIYOMI,
    ) = Extension.Available(
        name = name,
        pkgName = "pkg.${name.lowercase().replace(' ', '.')}",
        versionName = "$libVersionName.0",
        versionCode = 1L,
        libVersion = libVersionName.toLibVersionDouble(),
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        sources = emptyList(),
        apkUrl = "",
        iconUrl = "",
        store = extensionStore,
        libVersionName = libVersionName,
        sourceIdScheme = sourceIdScheme,
        isTorrent = false,
    )

    private fun String.toLibVersionDouble(): Double {
        val parts = split('.')
        return if (parts.size >= 2) {
            "${parts[0]}.${parts[1]}".toDouble()
        } else {
            toDouble()
        }
    }

    private companion object {
        val extensionStore = ExtensionStore(
            indexUrl = "https://example.org/repo.json",
            name = "Test store",
            badgeLabel = "test",
            signingKey = "",
            contact = ExtensionStore.Contact(
                website = "https://example.org",
                discord = null,
            ),
            format = ExtensionStoreFormat.V2,
        )
    }
}
