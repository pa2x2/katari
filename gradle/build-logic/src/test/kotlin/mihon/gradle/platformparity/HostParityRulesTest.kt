package mihon.gradle.platformparity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HostParityRulesTest {

    @Test
    fun `a shared binding container listed by only one production graph is reported`() {
        val findings = checkHostParity(
            listOf(
                HostParitySource(
                    "data/src/commonMain/kotlin/shared/di/SharedBindings.kt",
                    """
                        package shared.di

                        @BindingContainer
                        object DatabaseBindings

                        @BindingContainer
                        abstract class EntryHostBindings
                    """.trimIndent(),
                ),
                HostParitySource(
                    "app/src/main/java/app/di/AppGraph.kt",
                    """
                        package app.di

                        import shared.di.DatabaseBindings
                        import shared.di.EntryHostBindings

                        @DependencyGraph(
                            AppScope::class,
                            // DesktopOnlyBindings::class is listed by the desktop graph alone
                            bindingContainers = [
                                AppBindings::class,
                                DatabaseBindings::class,
                                EntryHostBindings::class,
                            ],
                        )
                        interface AppGraph
                    """.trimIndent(),
                ),
                HostParitySource(
                    "desktop/src/main/kotlin/desktop/di/DesktopGraph.kt",
                    """
                        package desktop.di

                        import shared.di.DatabaseBindings

                        @DependencyGraph(
                            AppScope::class,
                            excludes = [shared.di.EntryHostBindings::class],
                            bindingContainers = [DesktopBindings::class, DatabaseBindings::class],
                        )
                        interface DesktopGraph
                    """.trimIndent(),
                ),
                HostParitySource(
                    "app/src/test/java/app/di/TestGraph.kt",
                    """
                        package app.di

                        @DependencyGraph(AppScope::class, bindingContainers = [shared.di.DatabaseBindings::class])
                        interface TestGraph
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(
            listOf("app/src/main/java/app/di/AppGraph.kt:12 shared.di.EntryHostBindings"),
            findings.map { "${it.relativePath}:${it.lineNumber} ${it.reason.substringBefore(' ')}" },
        )
    }
}
