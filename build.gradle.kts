import mihon.gradle.configurations.VERIFY_TESTS_TASK_NAME
import mihon.gradle.extensionabi.ExtensionAbiCheckTask
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskProvider

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle)
    }
}

val extensionAbiChecker = configurations.create("extensionAbiChecker") {
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
    extensionAbiChecker(libs.japicmp)
}

plugins {
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineProfile) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.moko.resources) apply false
    alias(libs.plugins.sqldelight) apply false

    alias(mihonx.plugins.spotless)
}

val buildLogic: IncludedBuild = gradle.includedBuild("build-logic")

val legacySourceApiJar = layout.projectDirectory.file(
    "source-api/build/intermediates/aar_main_jar/androidMain/syncAndroidMainLibJars/classes.jar",
)
val coreCommonJar = layout.projectDirectory.file(
    "core/common/build/intermediates/aar_main_jar/release/syncReleaseLibJars/classes.jar",
)
val aniyomiSourceApiJar = layout.projectDirectory.file(
    "aniyomi-source-api/build/intermediates/aar_main_jar/release/syncReleaseLibJars/classes.jar",
)

fun registerExtensionAbiCheck(
    taskName: String,
    baseline: String,
    baselineLabel: String,
    expectedSha256Value: String,
    currentJarFiles: List<RegularFile>,
    buildTasks: List<String>,
    includedElements: List<String>,
    excludedElements: List<String>,
    compatibleChanges: List<String> = emptyList(),
    acceptedChangeEntries: List<String> = emptyList(),
): TaskProvider<ExtensionAbiCheckTask> = tasks.register<ExtensionAbiCheckTask>(taskName) {
    group = "verification"
    description = "Checks the extension runtime ABI against $baselineLabel"

    dependsOn(buildTasks)
    encodedBaseline.set(layout.projectDirectory.file("$baseline.jar.b64"))
    expectedSha256.set(expectedSha256Value)
    currentJars.from(currentJarFiles)
    includes.set(includedElements)
    excludes.set(excludedElements)
    compatibleChangeTypes.set(compatibleChanges)
    acceptedChanges.set(acceptedChangeEntries)
    japicmpClasspath.from(extensionAbiChecker)
    report.set(layout.buildDirectory.file("extension-abi/$taskName.txt"))
}

val legacySourceExcludes = listOf(
    "eu.kanade.tachiyomi.source.ConfigurableSource\$getSourcePreferences\$\$inlined\$get\$1",
    "eu.kanade.tachiyomi.source.ConfigurableSourceKt\$sourcePreferences\$\$inlined\$get\$1",
    "eu.kanade.tachiyomi.source.ConfigurableSourceKt\$sourcePreferences\$\$inlined\$get\$2",
    "eu.kanade.tachiyomi.source.online.HttpSource\$special\$\$inlined\$injectLazy\$1",
    "eu.kanade.tachiyomi.source.online.HttpSource\$special\$\$inlined\$injectLazy\$1\$1",
)
// Every extensions-lib stub declares it, but no host has had it and no extension calls it.
val stubOnlyNetworkHelperConstructor =
    "eu.kanade.tachiyomi.network.NetworkHelper#NetworkHelper(android.content.Context)"

// The app's JavaScript engine left core-common, so JavaScriptEngine is an abstract class the app binds and extensions
// inject, as Mihon documents it. None of 1,680 Keiyoushi and Yūzōnō extensions constructs it or uses it at all.
val injectedJavaScriptEngineChanges = listOf(
    "eu.kanade.tachiyomi.network.JavaScriptEngine:CLASS_NOW_ABSTRACT",
    "eu.kanade.tachiyomi.network.JavaScriptEngine.JavaScriptEngine(android.content.Context):CONSTRUCTOR_REMOVED",
    "eu.kanade.tachiyomi.network.JavaScriptEngine.evaluate(java.lang.String,kotlin.coroutines.Continuation)" +
        ":METHOD_NOW_ABSTRACT",
)

val verifyLegacySourceAbi16 = registerExtensionAbiCheck(
    taskName = "verifyLegacySourceAbi16",
    baseline = "source-api/abi/upstream-mihon-source-api-1.6",
    baselineLabel = "original upstream Mihon 1.6",
    expectedSha256Value = "2204a07ed2e89bcee8fac8808269808a00d629880bd8c5b720ad75c7c7901d90",
    currentJarFiles = listOf(legacySourceApiJar),
    buildTasks = listOf(":source-api:assemble"),
    includedElements = listOf("eu.kanade.tachiyomi.source.*"),
    excludedElements = legacySourceExcludes,
)
val verifyKeiyoushiSourceAbi16 = registerExtensionAbiCheck(
    taskName = "verifyKeiyoushiSourceAbi16",
    baseline = "source-api/abi/keiyoushi-extensions-lib-1.6-6e0c96cea8",
    baselineLabel = "Keiyoushi extensions-lib 1.6 (6e0c96cea8)",
    expectedSha256Value = "b77f2f2d01ca03a5362eb6477c272f0392ebf5725b62a17ac657b7e1ad9791a2",
    currentJarFiles = listOf(legacySourceApiJar, coreCommonJar),
    buildTasks = listOf(":source-api:assemble", ":core:common:syncReleaseLibJars"),
    includedElements = listOf(
        "eu.kanade.tachiyomi.source.*",
        "eu.kanade.tachiyomi.network.*",
        "eu.kanade.tachiyomi.util.*",
        "eu.kanade.tachiyomi.AppInfo",
    ),
    excludedElements = legacySourceExcludes + stubOnlyNetworkHelperConstructor,
    acceptedChangeEntries = injectedJavaScriptEngineChanges,
)

val aniyomiExcludes = listOf(
    stubOnlyNetworkHelperConstructor,
    // Every public member is inline, so extensions carry their own copies and never link against these classes.
    "eu.kanade.tachiyomi.util.CoroutinesExtensionsKt*",
    "eu.kanade.tachiyomi.util.JsonExtensionsKt*",
    // Kotlin's holders for 17's @Deprecated(level = ERROR) on internalData; only reflection reads them.
    "eu.kanade.tachiyomi.animesource.model.Hoster#getInternalData\$annotations()",
    "eu.kanade.tachiyomi.animesource.model.Video#getInternalData\$annotations()",
)
// The libs disagree on which source calls have default bodies; giving an abstract method one never breaks linking.
val aniyomiCompatibleChanges = listOf("METHOD_ABSTRACT_NOW_DEFAULT")

fun registerAniyomiSourceAbiCheck(
    taskName: String,
    baseline: String,
    baselineLabel: String,
    expectedSha256Value: String,
    additionalExcludes: List<String> = emptyList(),
    additionalCompatibleChanges: List<String> = emptyList(),
) = registerExtensionAbiCheck(
    taskName = taskName,
    baseline = "aniyomi-source-api/abi/$baseline",
    baselineLabel = baselineLabel,
    expectedSha256Value = expectedSha256Value,
    currentJarFiles = listOf(aniyomiSourceApiJar, coreCommonJar, legacySourceApiJar),
    buildTasks = listOf(
        ":aniyomi-source-api:syncReleaseLibJars",
        ":core:common:syncReleaseLibJars",
        ":source-api:assemble",
    ),
    includedElements = listOf(
        "eu.kanade.tachiyomi.animesource.*",
        "eu.kanade.tachiyomi.torrentutils.*",
        "eu.kanade.tachiyomi.network.*",
        "eu.kanade.tachiyomi.util.*",
        "eu.kanade.tachiyomi.AppInfo",
    ),
    excludedElements = aniyomiExcludes + additionalExcludes,
    compatibleChanges = aniyomiCompatibleChanges + additionalCompatibleChanges,
    acceptedChangeEntries = injectedJavaScriptEngineChanges,
)

val verifyAniyomiSourceAbi14 = registerAniyomiSourceAbiCheck(
    taskName = "verifyAniyomiSourceAbi14",
    baseline = "aniyomi-extensions-lib-14",
    baselineLabel = "Aniyomi extensions-lib 14",
    expectedSha256Value = "44b9dabf5bd14377a48a73e8ce3da0223e829407098e09547a174770608c9e49",
    additionalExcludes = listOf(
        // The RxJava 1 calls are left out of the host on purpose; no extension in use overrides or calls them.
        "eu.kanade.tachiyomi.animesource.*#fetch*(*)",
        "eu.kanade.tachiyomi.animesource.*#fetchSearchAnime(*,*,*)",
        // 14's Video data class had a different primary constructor; no extension destructures a Video.
        "eu.kanade.tachiyomi.animesource.model.Video#component3()",
        "eu.kanade.tachiyomi.animesource.model.Video#component4()",
        "eu.kanade.tachiyomi.animesource.model.Video#component5()",
        "eu.kanade.tachiyomi.animesource.model.Video#component6()",
    ),
    additionalCompatibleChanges = listOf(
        // Raised on every subclass of a class that lost an Rx call; a real removal still fails on the class itself.
        "METHOD_REMOVED_IN_SUPERCLASS",
        // japicmp misses that AnimeCatalogueSource gives 16's new abstract AnimeSource calls a body, so subclasses
        // must implement exactly what 14 required. 16, 17 and Komikku still check this change.
        "METHOD_ABSTRACT_ADDED_IN_IMPLEMENTED_INTERFACE",
    ),
)
val verifyAniyomiSourceAbi16 = registerAniyomiSourceAbiCheck(
    taskName = "verifyAniyomiSourceAbi16",
    baseline = "aniyomi-extensions-lib-16",
    baselineLabel = "Aniyomi extensions-lib 16",
    expectedSha256Value = "cf981398207aaf4249784ed3772e426dd0a9de27f3ca83d6851e6f45a8a047c0",
)
val verifyAniyomiSourceAbi17 = registerAniyomiSourceAbiCheck(
    taskName = "verifyAniyomiSourceAbi17",
    baseline = "aniyomi-extensions-lib-17",
    baselineLabel = "Aniyomi extensions-lib 17",
    expectedSha256Value = "7b4f6d648f556b337ab7b8a7c0e2ea22b64f693c40a9e395e701f42323c6d86a",
)
val verifyKomikkuAniyomiSourceAbi = registerAniyomiSourceAbiCheck(
    taskName = "verifyKomikkuAniyomiSourceAbi",
    baseline = "komikku-aniyomi-extensions-lib-f5961b5dfb",
    baselineLabel = "Komikku's Aniyomi extensions-lib fork (f5961b5dfb)",
    expectedSha256Value = "85913c74fa02583a6e2cfae28cf62d0bd1fd408eb32a412ef426d5f12ec726ac",
)

tasks.register("verifyLegacySourceAbi") {
    group = "verification"
    description = "Checks runtime ABI compatibility with supported Mihon and Keiyoushi manga extensions"
    dependsOn(
        verifyLegacySourceAbi16,
        verifyKeiyoushiSourceAbi16,
        ":source-compat:testReleaseUnitTest",
    )
}

tasks.register("verifyAniyomiSourceAbi") {
    group = "verification"
    description = "Checks runtime ABI compatibility with Aniyomi extensions built against extensions-lib 14, 16, 17 " +
        "and Komikku's fork"
    dependsOn(
        verifyAniyomiSourceAbi14,
        verifyAniyomiSourceAbi16,
        verifyAniyomiSourceAbi17,
        verifyKomikkuAniyomiSourceAbi,
    )
}

val releaseR8MappingDirectory = layout.projectDirectory.dir("app/build/outputs/mapping/release")

tasks.register("verifyExtensionRuntimeAbi") {
    group = "verification"
    description = "Checks that R8 preserves the runtime ABI used by external extensions"

    // The final mapping is post-processed by the Compose mapping task after R8 finishes.
    // Depend on the assembled artifact so clean builds cannot race that post-processing.
    dependsOn(":app:assembleRelease")

    val configurationFile = releaseR8MappingDirectory.file("configuration.txt")
    val mappingFile = releaseR8MappingDirectory.file("mapping.txt")
    val usageFile = releaseR8MappingDirectory.file("usage.txt")
    inputs.files(configurationFile, mappingFile, usageFile)

    doLast {
        val requiredKeepRules = listOf(
            "-keep class eu.kanade.tachiyomi.source.** { *; }",
            "-keep class eu.kanade.tachiyomi.network.** { *; }",
            "-keep class mihon.book.api.** { *; }",
            "-keep class eu.kanade.tachiyomi.animesource.** { *; }",
            "-keep class eu.kanade.tachiyomi.torrentutils.** { *; }",
            "-keep class fi.iki.elonen.** { *; }",
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
            "eu.kanade.tachiyomi.animesource",
            "eu.kanade.tachiyomi.torrentutils",
            "fi.iki.elonen",
        )
        val runtimeAbiClasses = listOf("eu.kanade.tachiyomi.AppInfo")
        // AppInfo reads these by reflection, so R8 sees no use of them.
        val reflectedRuntimeFields = mapOf(
            "eu.kanade.tachiyomi.BuildConfig" to listOf("VERSION_CODE", "VERSION_NAME"),
        )
        // R8 names the classes it synthesizes, e.g. for desugared lambdas, after their context class. They only exist in
        // its output, so no extension can link against them.
        val r8SyntheticMarkers = listOf("\$\$InternalSynthetic", "\$\$ExternalSynthetic")
        fun isRuntimeAbiClass(className: String) = r8SyntheticMarkers.none(className::contains) && (
            className in runtimeAbiClasses ||
                runtimeAbiPackagePrefixes.any { prefix ->
                    className == prefix || className.startsWith("$prefix.") || className.startsWith("$prefix\$")
                }
            )

        val removedRuntimeAbi = mutableListOf<String>()
        var removedClass: String? = null
        usageFile.asFile.forEachLine { line ->
            if (line.isNotBlank() && !line.first().isWhitespace()) {
                val className = line.removeSuffix(":")
                removedClass = className
                // usage.txt lists a class without a trailing colon when R8 removed the whole class.
                if (!line.endsWith(":") && (isRuntimeAbiClass(className) || className in reflectedRuntimeFields)) {
                    removedRuntimeAbi += className
                }
            } else {
                val className = removedClass ?: return@forEachLine
                val member = line.trim()
                val isRuntimeAbiMember = isRuntimeAbiClass(className) &&
                    (member.startsWith("public ") || member.startsWith("protected "))
                val isReflectedField = reflectedRuntimeFields[className].orEmpty()
                    .contains(member.substringAfterLast(' '))
                if (isRuntimeAbiMember || isReflectedField) {
                    removedRuntimeAbi += "$className:\n$line"
                }
            }
        }
        check(removedRuntimeAbi.isEmpty()) {
            "R8 removed parts of the external extension runtime ABI:\n" +
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

tasks.register(VERIFY_TESTS_TASK_NAME) {
    group = "verification"
    description = "Runs the build logic tests; running the task by name also verifies every module's tests"
    dependsOn(buildLogic.task(":test"))
}

tasks {
    wrapper {
        // Retry transient distribution download failures; regenerating the wrapper otherwise resets these.
        retries.set(3)
        retryBackOffMs.set(1000)
    }

    listOf("clean", "spotlessApply", "spotlessCheck").forEach { task ->
        named(task) {
            dependsOn(buildLogic.task(":$task"))
        }
    }
}
