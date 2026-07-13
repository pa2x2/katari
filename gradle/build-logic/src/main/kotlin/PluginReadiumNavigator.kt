import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.registerTransform
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val EMBEDDED_PHOTO_VIEW = "libs/PhotoView-2.3.0.jar"
private val containsEmbeddedPhotoView = Attribute.of(
    "mihon.readium.contains-embedded-photoview",
    Boolean::class.javaObjectType,
)

/**
 * Removes Readium's embedded PhotoView copy so the app can keep owning its PhotoView dependency.
 *
 * Readium ships PhotoView inside the navigator AAR instead of declaring it as a dependency. Katari
 * already uses the same library in manga and cover readers, which otherwise produces duplicate
 * classes at APK assembly time.
 */
class PluginReadiumNavigator : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        dependencies.attributesSchema.attribute(containsEmbeddedPhotoView)
        dependencies.artifactTypes.maybeCreate("aar").apply {
            attributes.attribute(containsEmbeddedPhotoView, true)
        }
        dependencies.registerTransform(StripReadiumPhotoView::class) {
            from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
            from.attribute(containsEmbeddedPhotoView, true)
            to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
            to.attribute(containsEmbeddedPhotoView, false)
        }
        configurations.configureEach {
            attributes.attribute(containsEmbeddedPhotoView, false)
        }
    }
}

abstract class StripReadiumPhotoView : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.startsWith("readium-navigator-")) {
            outputs.file(input)
            return
        }
        val output = outputs.file(input.name)
        stripEmbeddedPhotoView(input.inputStream(), output.outputStream())
    }
}

internal fun stripEmbeddedPhotoView(input: java.io.InputStream, output: java.io.OutputStream) {
    ZipInputStream(input.buffered()).use { source ->
        ZipOutputStream(output.buffered()).use { target ->
            generateSequence(source::getNextEntry).forEach { entry ->
                if (entry.name != EMBEDDED_PHOTO_VIEW) {
                    target.putNextEntry(
                        ZipEntry(entry.name).apply {
                            comment = entry.comment
                            extra = entry.extra
                            method = entry.method
                            if (entry.method == ZipEntry.STORED) {
                                size = entry.size
                                compressedSize = entry.compressedSize
                                crc = entry.crc
                            }
                        },
                    )
                    source.copyTo(target)
                    target.closeEntry()
                }
                source.closeEntry()
            }
        }
    }
}
