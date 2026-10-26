package eu.kanade.tachiyomi.extension.lnreader

import android.content.Context
import eu.kanade.tachiyomi.extension.file.FileExtensionKind
import eu.kanade.tachiyomi.extension.file.FileExtensionStorage
import eu.kanade.tachiyomi.extension.file.InstalledFileExtension
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.extension.kind.isNewerThanListed
import eu.kanade.tachiyomi.extension.kind.runsLibVersion
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.lnreader.LnReaderPluginDescription
import eu.kanade.tachiyomi.source.lnreader.LnReaderSource
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginFile
import kotlinx.serialization.encodeToString
import mihon.app.di.appGraph
import mihon.data.extension.model.lnreader.NetworkLnReaderPlugin
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceProviderKind
import tachiyomi.domain.source.identity.lnReaderExternalId
import java.io.File
import java.io.IOException

/**
 * LNReader plugins: JavaScript files from an LNReader index, each one a book source through the LNReader adapter. What
 * a plugin says about itself takes opening a runtime, seconds on a phone, so it's read once per installed file and kept
 * beside it, and app start opens none.
 */
internal object LnReaderExtensionKind : FileExtensionKind {

    override val sourceIdScheme = SourceIdScheme.LNREADER
    override val providerKind = SourceProviderKind.LNREADER_PLUGIN
    override val family = ExtensionFamily.LNREADER

    override fun runsLib(libVersionName: String): Boolean = family.runsLibVersion(libVersionName)

    // The only kind of its scheme
    override fun isBuiltFor(libVersionName: String): Boolean = true

    override fun isNewerThanRun(libVersionName: String): Boolean? = family.isNewerThanListed(libVersionName)

    override fun compareVersions(first: String, second: String): Int = compareDottedVersions(first, second)

    override suspend fun prepareInstall(context: Context, staged: InstalledFileExtension) {
        val graph = context.appGraph
        val description = graph.lnReaderSourceFactory.describe(staged.pluginFile(), graph.profileStore.currentProfileId)
        val manifest = staged.manifest
        check(description.id == manifest.id) { "The file is the plugin ${description.id}, not ${manifest.id}" }
        check(description.version == manifest.versionName) {
            "The file is version ${description.version}, not ${manifest.versionName} as its store lists it"
        }
        writeDescription(staged, description)
    }

    override suspend fun createSources(
        context: Context,
        extension: InstalledFileExtension,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource> {
        val graph = context.appGraph
        val factory = graph.lnReaderSourceFactory
        val file = extension.pluginFile()
        val description = readDescription(extension)
            ?: factory.describe(file, graph.profileStore.currentProfileId).also { writeDescription(extension, it) }
        val source = factory.create(
            file = file,
            key = keys.keyOf(SourceIdentity(sourceIdScheme, file.sourceId)),
            description = description,
            lang = extension.manifest.lang,
            customCss = extension.companion(NetworkLnReaderPlugin.CUSTOM_CSS)?.readText(),
            activeProfileId = { graph.profileStore.currentProfileId },
        )
        return listOf(source)
    }

    // A plugin reads its settings as it's constructed
    override fun reloadSettings(sources: List<UnifiedSource>) {
        sources.filterIsInstance<LnReaderSource>().forEach(LnReaderSource::reload)
    }

    private fun InstalledFileExtension.pluginFile() = LnReaderPluginFile(
        sourceId = lnReaderExternalId(manifest.id),
        name = manifest.name,
        code = code.readText(),
    )

    private fun readDescription(extension: InstalledFileExtension): LnReaderPluginDescription? {
        val file = File(extension.contentDir, DESCRIPTION_FILE).takeIf { it.isFile } ?: return null
        return FileExtensionStorage.json.decodeFromString<LnReaderPluginDescription>(file.readText())
    }

    /** Replaced at once, since a load may read it while another writes it. */
    private fun writeDescription(extension: InstalledFileExtension, description: LnReaderPluginDescription) {
        val target = File(extension.contentDir, DESCRIPTION_FILE)
        val staged = File(extension.contentDir, "$DESCRIPTION_FILE.part")
        staged.writeText(FileExtensionStorage.json.encodeToString(description))
        if (!staged.renameTo(target)) {
            staged.delete()
            throw IOException("Unable to write ${target.path}")
        }
    }

    private const val DESCRIPTION_FILE = "description.json"
}
