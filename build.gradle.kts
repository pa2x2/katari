import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import java.security.MessageDigest
import java.util.Base64

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle)
    }
}

val legacySourceAbiVerifier by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM),
        )
    }
}

dependencies {
    legacySourceAbiVerifier(libs.japicmp)
}

plugins {
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineProfile) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.moko.resources) apply false
    alias(libs.plugins.sqldelight) apply false

    alias(mihonx.plugins.spotless)
}

val buildLogic: IncludedBuild = gradle.includedBuild("build-logic")

val currentLegacySourceApiJar = layout.projectDirectory.file(
    "source-api/build/intermediates/aar_main_jar/androidMain/syncAndroidMainLibJars/classes.jar",
)

fun registerLegacySourceAbiCheck(
    taskName: String,
    baselineName: String,
    baselineLabel: String,
    expectedSha256: String,
    additionalExcludes: List<String> = emptyList(),
) = tasks.register<JavaExec>(taskName) {
    group = "verification"
    description = "Checks the current source-api against the $baselineLabel ABI"

    dependsOn(":source-api:assemble")
    classpath = legacySourceAbiVerifier
    mainClass.set("japicmp.JApiCmp")

    val encodedBaseline = layout.projectDirectory.file("source-api/abi/$baselineName.jar.b64")
    val decodedBaseline = layout.buildDirectory.file("legacy-source-abi/$baselineName.jar")
    inputs.file(encodedBaseline)
    inputs.file(currentLegacySourceApiJar)

    doFirst {
        val output = decodedBaseline.get().asFile
        output.parentFile.mkdirs()
        output.writeBytes(Base64.getMimeDecoder().decode(encodedBaseline.asFile.readBytes()))
        val actualSha256 = MessageDigest.getInstance("SHA-256")
            .digest(output.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(actualSha256 == expectedSha256) {
            "Legacy ABI baseline $baselineName has SHA-256 $actualSha256, expected $expectedSha256"
        }

        val excludedElements = listOf(
            "eu.kanade.tachiyomi.source.ConfigurableSource\$getSourcePreferences\$\$inlined\$get\$1",
            "eu.kanade.tachiyomi.source.ConfigurableSourceKt\$sourcePreferences\$\$inlined\$get\$1",
            "eu.kanade.tachiyomi.source.ConfigurableSourceKt\$sourcePreferences\$\$inlined\$get\$2",
            "eu.kanade.tachiyomi.source.online.HttpSource\$special\$\$inlined\$injectLazy\$1",
            "eu.kanade.tachiyomi.source.online.HttpSource\$special\$\$inlined\$injectLazy\$1\$1",
        ) + additionalExcludes

        args = listOf(
            "--old", output.absolutePath,
            "--new", currentLegacySourceApiJar.asFile.absolutePath,
            "--include", "eu.kanade.tachiyomi.source.*",
            "--exclude", excludedElements.joinToString(";"),
            "-a", "protected",
            "--include-synthetic",
            "--only-incompatible",
            "--ignore-missing-classes",
            "--error-on-binary-incompatibility",
        )
    }
}

val verifyLegacySourceAbi14 = registerLegacySourceAbiCheck(
    taskName = "verifyLegacySourceAbi14",
    baselineName = "upstream-mihon-source-api-1.4",
    baselineLabel = "original upstream Mihon 1.4",
    expectedSha256 = "fcb9fd3b0f246a88e248d5582a9ec88910502a2597586ac36698294344f8634f",
)
val verifyLegacySourceAbi16 = registerLegacySourceAbiCheck(
    taskName = "verifyLegacySourceAbi16",
    baselineName = "upstream-mihon-source-api-1.6",
    baselineLabel = "original upstream Mihon 1.6",
    expectedSha256 = "2204a07ed2e89bcee8fac8808269808a00d629880bd8c5b720ad75c7c7901d90",
    additionalExcludes = listOf(
        // The 1.4 baseline already requires this exact descriptor as a default method. Making the
        // 1.6 abstract declaration default again restores 1.4 bytecode without removing the 1.6 symbol.
        "eu.kanade.tachiyomi.source.Source#getPageList(eu.kanade.tachiyomi.source.model.SChapter,kotlin.coroutines.Continuation)",
    ),
)
val verifyKeiyoushiSourceAbi16 = registerLegacySourceAbiCheck(
    taskName = "verifyKeiyoushiSourceAbi16",
    baselineName = "keiyoushi-extensions-lib-1.6-6e0c96cea8",
    baselineLabel = "Keiyoushi extensions-lib 1.6 (6e0c96cea8)",
    expectedSha256 = "b77f2f2d01ca03a5362eb6477c272f0392ebf5725b62a17ac657b7e1ad9791a2",
    additionalExcludes = listOf(
        // The runtime keeps this method default to preserve original upstream 1.4 linkage. An
        // extension compiled against Keiyoushi's abstract declaration remains binary-compatible.
        "eu.kanade.tachiyomi.source.Source#getPageList(eu.kanade.tachiyomi.source.model.SChapter,kotlin.coroutines.Continuation)",
    ),
)

tasks.register("verifyLegacySourceAbi") {
    group = "verification"
    description = "Checks runtime ABI compatibility with supported Mihon and Keiyoushi manga extensions"
    dependsOn(
        verifyLegacySourceAbi14,
        verifyLegacySourceAbi16,
        verifyKeiyoushiSourceAbi16,
        ":source-compat:testDebugUnitTest",
    )
}

val releaseR8MappingDirectory = layout.projectDirectory.dir("app/build/outputs/mapping/release")

tasks.register("verifyExtensionRuntimeAbi") {
    group = "verification"
    description = "Checks that R8 preserves the runtime ABI used by external extensions"

    dependsOn(":app:minifyReleaseWithR8")

    val configurationFile = releaseR8MappingDirectory.file("configuration.txt")
    val mappingFile = releaseR8MappingDirectory.file("mapping.txt")
    val usageFile = releaseR8MappingDirectory.file("usage.txt")
    inputs.files(configurationFile, mappingFile, usageFile)

    doLast {
        val requiredKeepRules = listOf(
            "-keep class eu.kanade.tachiyomi.source.** { *; }",
            "-keep class eu.kanade.tachiyomi.network.** { *; }",
            "-keep class mihon.book.api.** { *; }",
        )
        val configuration = configurationFile.asFile.readText()
        val missingKeepRules = requiredKeepRules.filterNot(configuration::contains)
        check(missingKeepRules.isEmpty()) {
            "Release R8 configuration is missing extension runtime ABI keep rules:\n" +
                missingKeepRules.joinToString("\n")
        }

        val runtimeAbiPackagePrefixes = listOf(
            "eu.kanade.tachiyomi.source",
            "eu.kanade.tachiyomi.network",
            "mihon.book.api",
        )
        val removedRuntimeAbi = mutableListOf<String>()
        var runtimeApiClass: String? = null
        usageFile.asFile.forEachLine { line ->
            if (line.isNotBlank() && !line.first().isWhitespace()) {
                val className = line.removeSuffix(":")
                runtimeApiClass = className.takeIf {
                    runtimeAbiPackagePrefixes.any { prefix ->
                        className == prefix || className.startsWith("$prefix.") || className.startsWith("$prefix\$")
                    }
                }
            } else if (
                runtimeApiClass != null &&
                (line.trimStart().startsWith("public ") || line.trimStart().startsWith("protected "))
            ) {
                removedRuntimeAbi += "$runtimeApiClass:\n$line"
            }
        }
        check(removedRuntimeAbi.isEmpty()) {
            "R8 removed public or protected members from the external extension runtime ABI:\n" +
                removedRuntimeAbi.joinToString("\n")
        }

        val requiredRuntimeMembers = mapOf(
            "mihon.book.api.BookContentDescriptor" to listOf(
                "void <init>(java.lang.String,java.lang.String,java.lang.String,int," +
                    "kotlin.jvm.internal.DefaultConstructorMarker)",
            ),
        )
        val mappingLines = mappingFile.asFile.readLines()
        val missingRuntimeMembers = requiredRuntimeMembers.flatMap { (className, requiredMembers) ->
            val classStart = mappingLines.indexOfFirst { it.startsWith("$className -> ") }
            val classEnd = if (classStart >= 0) {
                (classStart + 1..<mappingLines.size).firstOrNull { index ->
                    val line = mappingLines[index]
                    line.isNotBlank() && !line.first().isWhitespace() && line.contains(" -> ") && line.endsWith(":")
                } ?: mappingLines.size
            } else {
                classStart
            }
            val classMapping = if (classStart >= 0) mappingLines.subList(classStart, classEnd) else emptyList()
            requiredMembers.filterNot { requiredMember ->
                classMapping.any { it.contains(requiredMember) }
            }.map { "$className#$it" }
        }
        check(missingRuntimeMembers.isEmpty()) {
            "Release APK is missing members required by existing extensions:\n" +
                missingRuntimeMembers.joinToString("\n")
        }
    }
}

tasks.register("publishEntrySdkToMavenLocal") {
    group = "publishing"
    description = "Publishes every Entry SDK artifact with the same version to Maven Local"
    dependsOn(
        ":core:common:publishToMavenLocal",
        ":book-api:publishToMavenLocal",
        ":entry-source-api:publishToMavenLocal",
    )
}

tasks {
    listOf("clean", "spotlessApply", "spotlessCheck").forEach { task ->
        named(task) {
            dependsOn(buildLogic.task(":$task"))
        }
    }
}
