package mihon.gradle.tasks

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
                    alias(mihonx.plugins.android.application)
                }

                dependencies {
                    implementation(projects.entryInteractions)
                    implementation(projects.entryInteractions.manga)
                    implementation(projects.entryInteractions.spi)
                }
            """.trimIndent(),
            additionalFiles = mapOf(
                "desktop/build.gradle.kts" to """
                    plugins {
                        alias(mihonx.plugins.desktop.application)
                    }

                    dependencies {
                        implementation(projects.entryInteractions)
                        implementation(projects.entryInteractions.api)
                    }
                """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "app/build.gradle.kts:7: app must consume Entry features through " +
            "projects.entryInteractions, not: projects.entryInteractions.manga"
        error.message shouldContain "app/build.gradle.kts:8"
        error.message shouldContain "desktop/build.gradle.kts:7: desktop must consume Entry features through " +
            "projects.entryInteractions, not: projects.entryInteractions.api"
    }

    @Test
    fun `generic code cannot import a type module package`() {
        createBaseFixture(
            appSource = """
                package app

                import mihon.entry.interactions.manga.MangaOpenProcessor

                class AppFeature
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "direct import across Entry interaction boundary"
        error.message shouldContain "mihon.entry.interactions.manga."
    }

    @Test
    fun `multiplatform modules stay checked and findings name the real file`() {
        createBaseFixture(
            additionalFiles = mapOf(
                "domain/src/commonMain/kotlin/domain/Feature.kt" to """
                    package domain

                    expect class Feature
                """,
                "domain/src/androidMain/kotlin/domain/Feature.kt" to """
                    package domain

                    import mihon.entry.interactions.manga.MangaOpenProcessor

                    actual class Feature
                """,
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "domain/src/androidMain/kotlin/domain/Feature.kt"
        error.message shouldContain "direct import across Entry interaction boundary"
    }

    @Test
    fun `type module processor implementations must remain internal`() {
        createBaseFixture(
            mangaProcessorSource = """
                package mihon.entry.interactions.manga

                import mihon.entry.interactions.EntryOpenProcessor

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
                "app/src/main/java/mihon/app/di/AppBindings.kt" to
                    """
                        package mihon.app.di

                        import eu.kanade.tachiyomi.source.entry.EntryType
                        import mihon.entry.interactions.manga.MangaOpenProcessor

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
                "entry-interactions/manga/src/main/java/mihon/entry/interactions/manga/download/DownloadCache.kt" to
                    """
                        package mihon.entry.interactions.manga.download

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
                "entry-interactions/api/src/main/java/mihon/entry/interactions/migration/host/" +
                    "EntryMigrationPreparationHost.kt" to
                    """
                        package mihon.entry.interactions.host

                        interface EntryMigrationPreparationHost
                    """.trimIndent(),
                "entry-interactions/src/main/java/mihon/entry/interactions/download/EntryDownloadFeature.kt" to
                    """
                        package mihon.entry.interactions

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
                "entry-interactions/src/main/java/mihon/entry/interactions/merge/EntryMergeCoordinator.kt" to
                    """
                        package mihon.entry.interactions

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
                alias(mihonx.plugins.android.application)
            }

            dependencies {
                implementation(projects.entryInteractions)
            }
        """.trimIndent(),
        mangaBuildGradle: String = """
            dependencies {
                implementation(projects.entryInteractions.spi)
            }
        """.trimIndent(),
        mangaProcessorSource: String = """
            package mihon.entry.interactions.manga

            import mihon.entry.interactions.EntryOpenProcessor

            internal class MangaOpenProcessor : EntryOpenProcessor
        """.trimIndent(),
        appSource: String = """
            package app

            class AppFeature
        """.trimIndent(),
        additionalFiles: Map<String, String> = emptyMap(),
    ) {
        write(
            "entry-interactions/spi/src/main/java/mihon/entry/interactions/runtime/EntryInteractions.kt",
            """
                package mihon.entry.interactions

                interface EntryOpenProcessor

                interface EntryOpenInteraction

                interface EntryInteractionRegistry
            """.trimIndent(),
        )
        write(
            "entry-interactions/manga/build.gradle.kts",
            mangaBuildGradle,
        )
        write(
            "entry-interactions/manga/src/main/java/mihon/entry/interactions/manga/MangaOpenProcessor.kt",
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
