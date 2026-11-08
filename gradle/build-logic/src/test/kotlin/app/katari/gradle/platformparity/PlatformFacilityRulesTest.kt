package app.katari.gradle.platformparity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlatformFacilityRulesTest {

    @Test
    fun `a desktop class answering a shared contract that isn't a facility is reported unless it is a bound element`() {
        val findings = checkPlatformFacilities(
            FACILITIES + listOf(
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/DesktopClock.kt",
                    """
                        package desktop

                        import feature.Clock
                        import feature.Vibration

                        @ContributesBinding(AppScope::class)
                        class DesktopClock : Clock

                        @ContributesBinding(AppScope::class)
                        class NoVibration : Vibration
                    """.trimIndent(),
                ),
                // Bound to desktop, but a single binding: it stands in for the shared answer all the same.
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/SwingClock.kt",
                    """
                        package desktop

                        import feature.Clock
                        import javax.swing.JFrame

                        @ContributesBinding(AppScope::class)
                        class SwingClock(private val frame: JFrame) : Clock
                    """.trimIndent(),
                ),
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/TrayEngine.kt",
                    """
                        package desktop

                        import feature.Engine
                        import javax.swing.JFrame

                        @ContributesIntoSet(AppScope::class)
                        class TrayEngine(private val frame: JFrame) : Engine
                    """.trimIndent(),
                ),
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/PlainEngine.kt",
                    """
                        package desktop

                        import feature.Engine

                        @ContributesIntoSet(AppScope::class)
                        class PlainEngine : Engine
                    """.trimIndent(),
                ),
            ),
            APP_BUILD_FILES,
            PlatformApis(
                desktop = PlatformApiNames(classes = setOf("javax.swing.JFrame"), callables = emptySet()),
                android = PlatformApiNames.EMPTY,
                androidStandIns = PlatformApiNames.EMPTY,
            ),
        )

        assertEquals(
            listOf(
                "desktop/src/main/kotlin/desktop/DesktopClock.kt:7: DesktopClock answers feature.Clock for desktop " +
                    "only, but feature.Clock isn't a @PlatformFacility; bind its shared implementation instead",
                "desktop/src/main/kotlin/desktop/PlainEngine.kt:6: PlainEngine answers feature.Engine for desktop " +
                    "only, but feature.Engine isn't a @PlatformFacility; bind its shared implementation instead",
                "desktop/src/main/kotlin/desktop/SwingClock.kt:7: SwingClock answers feature.Clock for desktop " +
                    "only, but feature.Clock isn't a @PlatformFacility; bind its shared implementation instead",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }

    @Test
    fun `a shared default for a facility is reported`() {
        val findings = checkPlatformFacilities(
            FACILITIES + listOf(
                PlatformSource(
                    "feature/src/commonMain/kotlin/feature/SilentVibration.kt",
                    """
                        package feature

                        class SilentVibration : Vibration

                        @BindingContainer
                        interface VibrationBindings {
                            @Binds
                            val SilentVibration.vibration: Vibration
                        }
                    """.trimIndent(),
                ),
            ),
            APP_BUILD_FILES,
            PlatformApis.EMPTY,
        )

        assertEquals(
            listOf(
                "feature/src/commonMain/kotlin/feature/SilentVibration.kt:3: SilentVibration implements the " +
                    "@PlatformFacility feature.Vibration in shared code; each platform implements a facility itself",
                "feature/src/commonMain/kotlin/feature/SilentVibration.kt:7: a shared binding binds the " +
                    "@PlatformFacility feature.Vibration; bind it in each platform's graph",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }

    private companion object {
        val FACILITIES = listOf(
            PlatformSource(
                "feature/src/commonMain/kotlin/feature/Contracts.kt",
                """
                    package feature

                    interface Clock

                    interface Engine

                    @PlatformFacility
                    interface Vibration
                """.trimIndent(),
            ),
        )
    }
}
