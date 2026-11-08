package app.katari.gradle.tasks

import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class EntryInteractionBoundaryCheckTaskTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `valid interaction layout passes`() {
        createBaseFixture()

        runBoundaryCheck()
    }

    @Test
    fun `application modules cannot depend on type modules or interaction infrastructure directly`() {
        createBaseFixture(
            appBuildGradle = """
                plugins {
                    alias(katari.plugins.android.application)
                }

                dependencies {
                    implementation(projects.features.entryInteractions)
                    implementation(projects.features.entryInteractions.manga)
                    implementation(projects.features.entryInteractions.spi)
                }
            """.trimIndent(),
            additionalFiles = mapOf(
                "desktop/build.gradle.kts" to """
                    plugins {
                        alias(katari.plugins.desktop.application)
                    }

                    dependencies {
                        implementation(projects.features.entryInteractions)
                        implementation(projects.features.entryInteractions.api)
                    }
                """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "app/build.gradle.kts:7: app must consume Entry features through " +
            "projects.features.entryInteractions, not: projects.features.entryInteractions.manga"
        error.message shouldContain "app/build.gradle.kts:8"
        error.message shouldContain "desktop/build.gradle.kts:7: desktop must consume Entry features through " +
            "projects.features.entryInteractions, not: projects.features.entryInteractions.api"
    }

    @Test
    fun `generic code cannot import a type module package`() {
        createBaseFixture(
            appSource = """
                package app

                import app.katari.entry.interactions.manga.MangaOpenProcessor

                class AppFeature
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "direct import across Entry interaction boundary"
        error.message shouldContain "app.katari.entry.interactions.manga."
    }

    @Test
    fun `multiplatform modules stay checked and findings name the real file`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "core/domain/src/commonMain/kotlin/domain/Feature.kt" to """
                    package domain

                    expect class Feature
                """,
                "core/domain/src/androidMain/kotlin/domain/Feature.kt" to """
                    package domain

                    import app.katari.entry.interactions.manga.MangaOpenProcessor

                    actual class Feature
                """,
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "core/domain/src/androidMain/kotlin/domain/Feature.kt"
        error.message shouldContain "direct import across Entry interaction boundary"
    }

    @Test
    fun `multiplatform type modules and the root module's shared code stay checked`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "features/entry-interactions/manga/src/commonMain/kotlin/app/katari/entry/interactions/manga/state/" +
                    "MangaConsumptionProcessor.kt" to """
                        package app.katari.entry.interactions.manga.state

                        import app.katari.entry.interactions.EntryOpenProcessor

                        class MangaConsumptionProcessor : EntryOpenProcessor
                    """,
                "features/entry-interactions/src/commonMain/kotlin/app/katari/entry/interactions/state/" +
                    "EntryConsumption.kt" to """
                    package app.katari.entry.interactions.state

                    import app.katari.entry.interactions.manga.state.MangaConsumptionProcessor

                    class EntryConsumption
                """,
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "features/entry-interactions/manga/src/commonMain/kotlin/app/katari/entry/interactions/manga/state/" +
            "MangaConsumptionProcessor.kt:5: type-module processor must remain internal: MangaConsumptionProcessor"
        error.message shouldContain
            "features/entry-interactions/src/commonMain/kotlin/app/katari/entry/interactions/state/" +
            "EntryConsumption.kt:3: root Entry interactions must not import manga type-module symbols"
    }

    @Test
    fun `a type module whose sources the check doesn't read fails`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "features/entry-interactions/comic/build.gradle.kts" to "",
                "features/entry-interactions/comic/src/jvmMain/kotlin/app/katari/entry/interactions/comic/" +
                    "ComicOpenProcessor.kt" to
                    """
                    package app.katari.entry.interactions.comic

                    class ComicOpenProcessor
                """,
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "features/entry-interactions/comic: type module has none of the source roots the check reads"
    }

    @Test
    fun `type module processor implementations must remain internal`() {
        createBaseFixture(
            mangaProcessorSource = """
                package app.katari.entry.interactions.manga

                import app.katari.entry.interactions.EntryOpenProcessor

                class MangaOpenProcessor : EntryOpenProcessor
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "type-module processor must remain internal"
        error.message shouldContain "MangaOpenProcessor"
    }

    @Test
    fun `application composition cannot authorize type-specific interaction behavior`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "app/src/main/java/app/katari/android/di/AppBindings.kt" to
                    """
                        package app.katari.android.di

                        import eu.kanade.tachiyomi.source.entry.EntryType
                        import app.katari.entry.interactions.manga.MangaOpenProcessor

                        fun processor(type: EntryType): Any? = when (type) {
                            EntryType.MANGA -> MangaOpenProcessor()
                            else -> null
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "suspicious EntryType processing branch reaches across Entry interaction boundaries"
    }

    @Test
    fun `type module public api parser ignores class literals in annotations`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "features/entry-interactions/manga/src/main/java/app/katari/entry/interactions/manga/download/" +
                    "DownloadCache.kt" to
                    """
                        package app.katari.entry.interactions.manga.download

                        internal class DownloadCache

                        private class RootDirectory(
                            @Serializable(with = UniFileAsStringSerializer::class)
                            val dir: String?,
                        )

                        private object UniFileAsStringSerializer
                    """.trimIndent(),
            ),
        )

        runBoundaryCheck()
    }

    @Test
    fun `unrelated feature and application api cannot borrow Migration host ports`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "features/entry-interactions/api/src/main/java/app/katari/entry/interactions/migration/host/" +
                    "EntryMigrationPreparationHost.kt" to
                    """
                        package app.katari.entry.interactions.host

                        interface EntryMigrationPreparationHost
                    """.trimIndent(),
                "features/entry-interactions/src/main/java/app/katari/entry/interactions/download/" +
                    "EntryDownloadFeature.kt" to
                    """
                        package app.katari.entry.interactions

                        class EntryDownloadFeature(private val host: EntryMigrationPreparationHost)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "EntryMigrationPreparationHost is an application host port reserved for the root Migration coordinator"
    }

    @Test
    fun `Merge implementation cannot use ambient profile authority or concrete type gates`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "features/entry-interactions/src/main/java/app/katari/entry/interactions/merge/" +
                    "EntryMergeCoordinator.kt" to
                    """
                        package app.katari.entry.interactions

                        class EntryMergeCoordinator(
                            private val profiles: ActiveProfileProvider,
                        ) {
                            val supported = EntryType.AUDIO
                            val profileId = profiles.activeProfileId
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "not ambient profile authority: ActiveProfileProvider"
        error.message shouldContain "not ambient profile authority: activeProfileId"
        error.message shouldContain "cannot gate behavior on a concrete current EntryType: AUDIO"
    }

    private fun createBaseFixture(
        appBuildGradle: String = """
            plugins {
                alias(katari.plugins.android.application)
            }

            dependencies {
                implementation(projects.features.entryInteractions)
            }
        """.trimIndent(),
        mangaBuildGradle: String = """
            dependencies {
                implementation(projects.features.entryInteractions.spi)
            }
        """.trimIndent(),
        mangaProcessorSource: String = """
            package app.katari.entry.interactions.manga

            import app.katari.entry.interactions.EntryOpenProcessor

            internal class MangaOpenProcessor : EntryOpenProcessor
        """.trimIndent(),
        appSource: String = """
            package app

            class AppFeature
        """.trimIndent(),
        additionalFiles: Map<String, String> = emptyMap(),
    ) {
        write(
            "features/entry-interactions/spi/src/main/java/app/katari/entry/interactions/runtime/EntryInteractions.kt",
            """
                package app.katari.entry.interactions

                interface EntryOpenProcessor

                interface EntryOpenInteraction

                interface EntryInteractionRegistry
            """.trimIndent(),
        )
        write(
            "features/entry-interactions/manga/build.gradle.kts",
            mangaBuildGradle,
        )
        write(
            "features/entry-interactions/manga/src/main/java/app/katari/entry/interactions/manga/MangaOpenProcessor.kt",
            mangaProcessorSource,
        )
        write(
            "app/build.gradle.kts",
            appBuildGradle,
        )
        write(
            "app/src/main/java/app/AppFeature.kt",
            appSource,
        )
        additionalFiles.forEach { (path, content) ->
            write(path, content)
        }
    }

    private fun runBoundaryCheck() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register(
            "checkEntryInteractionBoundaries",
            EntryInteractionBoundaryCheckTask::class.java,
        ) {
            repositoryRoot.set(tempDir.toFile())
        }.get()

        task.action()
    }

    private fun write(relativePath: String, content: String) {
        val file = tempDir.resolve(relativePath)
        file.parent.createDirectories()
        file.writeText(content.trimIndent() + "\n")
    }
}
