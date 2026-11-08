package app.katari.gradle.platformparity

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/** Marks the consumable configuration through which an app hands `indexPlatformApis` its platform's libraries. */
val PLATFORM_API_LIBRARIES_ATTRIBUTE: Attribute<String> = Attribute.of(
    "app.katari.platformApiLibraries",
    String::class.java,
)

const val ANDROID_PLATFORM_API_LIBRARIES = "android"

/**
 * Hands the root's `indexPlatformApis` the names of `android.jar` and of [variantName]'s external libraries, read here
 * because only this project's Android plugin turns its AARs into class jars. The repository's own modules are left
 * out: the checks read their declarations from the sources, so the index never waits for them to compile.
 */
fun Project.exposeAndroidPlatformApiLibraries(variantName: String) {
    val androidComponents = extensions.getByType<ApplicationAndroidComponentsExtension>()
    val index = tasks.register<PlatformApiLibraryIndexTask>("indexAndroidPlatformApiLibraries") {
        description = "Indexes the names android.jar and the $variantName build's libraries declare."
        classpath.from(androidComponents.sdkComponents.bootClasspath)
        names.set(layout.buildDirectory.file("platform-apis/android-libraries.txt"))
    }
    androidComponents.onVariants(androidComponents.selector().withName(variantName)) { variant ->
        index.configure {
            classpath.from(
                variant.runtimeConfiguration.incoming.artifactView {
                    attributes.attribute(ARTIFACT_TYPE, CLASSES_JAR)
                    componentFilter { it is ModuleComponentIdentifier }
                }.files,
            )
        }
    }
    configurations.consumable("androidPlatformApiLibraries") {
        attributes.attribute(PLATFORM_API_LIBRARIES_ATTRIBUTE, ANDROID_PLATFORM_API_LIBRARIES)
        outgoing.artifact(index.flatMap { it.names })
    }
}

@CacheableTask
abstract class PlatformApiLibraryIndexTask : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:OutputFile
    abstract val names: RegularFileProperty

    @TaskAction
    fun action() {
        names.get().asFile.bufferedWriter().use { indexClasspath(classpath).write(it) }
    }
}

private val ARTIFACT_TYPE: Attribute<String> = Attribute.of("artifactType", String::class.java)

private const val CLASSES_JAR = "android-classes-jar"
