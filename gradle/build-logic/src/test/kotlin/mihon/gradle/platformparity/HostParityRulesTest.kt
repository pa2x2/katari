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

    @Test
    fun `a host entry point one host never calls is reported`() {
        val findings = checkHostParity(
            listOf(
                HostParitySource(
                    "app-ui/src/commonMain/kotlin/shared/host/AppStart.kt",
                    """
                        package shared.host

                        object AppStart {
                            @HostEntryPoint
                            fun installLogging() = Unit

                            @HostEntryPoint
                            suspend fun checkForUpdates() = Unit
                        }

                        @HostEntryPoint
                        @Composable
                        fun Home(onReady: () -> Unit) = Unit
                    """.trimIndent(),
                ),
                HostParitySource(
                    "app/src/main/java/app/App.kt",
                    """
                        package app

                        fun onCreate() {
                            AppStart.installLogging()
                            launch { AppStart.checkForUpdates() }
                            setContent { Home(onReady = {}) }
                        }
                    """.trimIndent(),
                ),
                HostParitySource(
                    "desktop/src/main/kotlin/desktop/Main.kt",
                    """
                        package desktop

                        // The update check is not called here: AppStart.checkForUpdates()
                        fun main() {
                            AppStart.installLogging()
                            application { Home(onReady = {}) }
                        }
                    """.trimIndent(),
                ),
                HostParitySource(
                    "desktop/src/test/kotlin/desktop/MainTest.kt",
                    "fun test() = AppStart.checkForUpdates()",
                ),
            ),
        )

        assertEquals(
            listOf("app-ui/src/commonMain/kotlin/shared/host/AppStart.kt:7 checkForUpdates desktop"),
            findings.map {
                "${it.relativePath}:${it.lineNumber} ${it.reason.substringBefore(' ')} " +
                    it.reason.substringAfter("but the ").substringBefore(' ')
            },
        )
    }

    @Test
    fun `a host providing a local the composition root provides, or a platform link handler, is reported`() {
        val findings = checkHostParity(
            listOf(
                HostParitySource(
                    "app-ui/src/commonMain/kotlin/shared/host/AppUiRoot.kt",
                    """
                        package shared.host

                        @HostEntryPoint
                        @Composable
                        fun AppUiRoot(graph: AppUiGraph, content: @Composable () -> Unit) {
                            CompositionLocalProvider(
                                LocalAppUiGraph provides graph,
                                LocalUriHandler provides LinkOpenerUriHandler(graph.linkOpener),
                                content = content,
                            )
                        }
                    """.trimIndent(),
                ),
                HostParitySource(
                    "desktop/src/main/kotlin/desktop/window/Window.kt",
                    """
                        package desktop.window

                        @Composable
                        fun WindowContent(graph: DesktopGraph, content: @Composable () -> Unit) {
                            CompositionLocalProvider(
                                LocalPlatformContext provides WindowPlatformContext(window),
                                // LocalUriHandler provides is only mentioned here
                                LocalAppUiGraph provides graph,
                            ) {
                                AppUiRoot(graph, content)
                            }
                        }
                    """.trimIndent(),
                ),
                HostParitySource(
                    "app/src/main/java/app/ViewExtensions.kt",
                    "fun setComposeContent() = setContent { AppUiRoot(appGraph) {} }",
                ),
                HostParitySource(
                    "translation/ui/src/androidMain/kotlin/translation/Host.kt",
                    """
                        @Composable
                        fun Host() = CompositionLocalProvider(LocalClipboardManager provides manager) {}
                    """.trimIndent(),
                ),
                HostParitySource(
                    "app/src/test/java/app/HostTest.kt",
                    "fun test() = CompositionLocalProvider(LocalAppUiGraph provides graph) {}",
                ),
            ),
        )

        assertEquals(
            listOf(
                "desktop/src/main/kotlin/desktop/window/Window.kt:8 LocalAppUiGraph",
                "translation/ui/src/androidMain/kotlin/translation/Host.kt:2 LocalClipboardManager",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber} ${it.reason.substringBefore(' ')}" },
        )
    }
}
