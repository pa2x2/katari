package mihon.data.extension.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme
import kotlin.io.encoding.Base64

class NetworkIndexLayoutsTest {

    @Test
    fun `an aniyomi index reads in its own layout with or without torrents and a katari one in katari's`() {
        val withTorrent = read(Base64.decode(YUZONO_WITH_TORRENT)).toListings()
        val withoutTorrent = read(Base64.decode(YUZONO_WITHOUT_TORRENT)).toListings()
        val katari = read(ProtoBuf.encodeToByteArray(katariStore())).toListings()

        withTorrent.map { Triple(it.name, it.isTorrent, it.sources.single().id) } shouldContainExactly listOf(
            Triple("AnimeOnsen", false, 8542735178285060053),
            Triple("Subsplease", true, 8766955949256845198),
        )
        withTorrent.all { it.sourceIdScheme == SourceIdScheme.ANIYOMI } shouldBe true
        withTorrent.first().sources.single().supportedEntryTypes shouldBe setOf(EntryType.ANIME)
        withoutTorrent.map { it.name to it.sources.size } shouldContainExactly listOf("AnimeOnsen" to 1)
        withoutTorrent.single().sourceIdScheme shouldBe SourceIdScheme.ANIYOMI
        katari.map { it.name to it.sourceIdScheme } shouldContainExactly listOf("Example" to SourceIdScheme.TACHIYOMI)
    }

    private fun read(bytes: ByteArray): NetworkV2ExtensionStore = readInFittingLayout(
        katari = { ProtoBuf.decodeFromByteArray<NetworkExtensionStore>(bytes) },
        aniyomi = { ProtoBuf.decodeFromByteArray<AniyomiNetworkExtensionStore>(bytes) },
        extensionList = { it.extensionList },
    )

    private fun NetworkV2ExtensionStore.toListings() = extensionList!!.toAvailableExtensions(
        toExtensionStore(INDEX_URL),
    )

    private fun katariStore() = NetworkExtensionStore(
        name = "Example",
        badgeLabel = "Example",
        signingKey = "",
        contact = NetworkExtensionStore.Contact(website = "https://example.invalid", discord = null),
        extensionList = NetworkExtensionStore.ExtensionList(
            listOf(
                NetworkExtensionStore.Extension(
                    name = "Example",
                    packageName = "example.extension",
                    resources = NetworkExtensionStore.Resources(apkUrl = "", iconUrl = ""),
                    extensionLib = "1.6",
                    versionCode = 1L,
                    versionName = "1.6.1",
                    contentWarning = NetworkExtensionStore.ContentWarning.SAFE,
                    sources = listOf(NetworkExtensionStore.Source(id = 1L, name = "Example", language = "en")),
                ),
            ),
        ),
        extensionListUrl = null,
    )

    private companion object {
        const val INDEX_URL = "https://github.com/yuzono/anime-repo/raw/repo/index.pb"

        // Yūzōnō's index.pb of 2026-10-24, cut down to AnimeOnsen and the torrent extension Subsplease
        const val YUZONO_WITH_TORRENT =
            "CglZxat6xY1uxY0SA1nFqxpAY2JlYzEyMWFhODJlYmIwMmFhYTczODA2OTkyZTAzNjhhOTdkNDdiNTQ1MWVkNjUyNDgxNmQwMzA4" +
                "NGM0NTkwNSI5ChhodHRwczovL3l1em9uby5naXRodWIuaW8SHWh0dHBzOi8vZGlzY29yZC5nZy84NU1aaFVYNjg4qgbsBQr1AgoK" +
                "QW5pbWVPbnNlbhIxZXUua2FuYWRlLnRhY2hpeW9taS5hbmltZWV4dGVuc2lvbi5hbGwuYW5pbWVvbnNlbhrrAQppaHR0cHM6Ly9y" +
                "YXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3l1em9uby9hbmltZS1yZXBvL3JlZnMvaGVhZHMvcmVwby9hcGsvYW5peW9taS1hbGwu" +
                "YW5pbWVvbnNlbi12MTQuMTAuYXBrEn5odHRwczovL3Jhdy5naXRodWJ1c2VyY29udGVudC5jb20veXV6b25vL2FuaW1lLXJlcG8v" +
                "cmVmcy9oZWFkcy9yZXBvL2ljb24vZXUua2FuYWRlLnRhY2hpeW9taS5hbmltZWV4dGVuc2lvbi5hbGwuYW5pbWVvbnNlbi5wbmci" +
                "AjE0KAoyBTE0LjEwOAFKNwjVn6j/oYz5xnYSCkFuaW1lT25zZW4aA2FsbCIaaHR0cHM6Ly93d3cuYW5pbWVvbnNlbi54eXoK8QIK" +
                "ClN1YnNwbGVhc2USMWV1LmthbmFkZS50YWNoaXlvbWkuYW5pbWVleHRlbnNpb24uYWxsLnN1YnNwbGVhc2Ua6gEKaGh0dHBzOi8v" +
                "cmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS95dXpvbm8vYW5pbWUtcmVwby9yZWZzL2hlYWRzL3JlcG8vYXBrL2FuaXlvbWktYWxs" +
                "LnN1YnNwbGVhc2UtdjE0LjguYXBrEn5odHRwczovL3Jhdy5naXRodWJ1c2VyY29udGVudC5jb20veXV6b25vL2FuaW1lLXJlcG8v" +
                "cmVmcy9oZWFkcy9yZXBvL2ljb24vZXUua2FuYWRlLnRhY2hpeW9taS5hbmltZWV4dGVuc2lvbi5hbGwuc3Vic3BsZWFzZS5wbmci" +
                "AjE0KAgyBDE0Ljg4AUABSjMIjs/O58j/ntV5EgpTdWJzcGxlYXNlGgNhbGwiFmh0dHBzOi8vc3Vic3BsZWFzZS5vcmc="

        // The same, cut down to AnimeOnsen
        const val YUZONO_WITHOUT_TORRENT =
            "CglZxat6xY1uxY0SA1nFqxpAY2JlYzEyMWFhODJlYmIwMmFhYTczODA2OTkyZTAzNjhhOTdkNDdiNTQ1MWVkNjUyNDgxNmQwMzA4" +
                "NGM0NTkwNSI5ChhodHRwczovL3l1em9uby5naXRodWIuaW8SHWh0dHBzOi8vZGlzY29yZC5nZy84NU1aaFVYNjg4qgb4Agr1AgoK" +
                "QW5pbWVPbnNlbhIxZXUua2FuYWRlLnRhY2hpeW9taS5hbmltZWV4dGVuc2lvbi5hbGwuYW5pbWVvbnNlbhrrAQppaHR0cHM6Ly9y" +
                "YXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3l1em9uby9hbmltZS1yZXBvL3JlZnMvaGVhZHMvcmVwby9hcGsvYW5peW9taS1hbGwu" +
                "YW5pbWVvbnNlbi12MTQuMTAuYXBrEn5odHRwczovL3Jhdy5naXRodWJ1c2VyY29udGVudC5jb20veXV6b25vL2FuaW1lLXJlcG8v" +
                "cmVmcy9oZWFkcy9yZXBvL2ljb24vZXUua2FuYWRlLnRhY2hpeW9taS5hbmltZWV4dGVuc2lvbi5hbGwuYW5pbWVvbnNlbi5wbmci" +
                "AjE0KAoyBTE0LjEwOAFKNwjVn6j/oYz5xnYSCkFuaW1lT25zZW4aA2FsbCIaaHR0cHM6Ly93d3cuYW5pbWVvbnNlbi54eXo="
    }
}
