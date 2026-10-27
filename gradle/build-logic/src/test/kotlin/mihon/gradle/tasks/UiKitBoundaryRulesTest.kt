package mihon.gradle.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiKitBoundaryRulesTest {

    @Test
    fun `each styling decision made outside the kit is reported on its line`() {
        val findings = check(
            "tts/ui/src/main/java/Screen.kt",
            """
            import androidx.compose.material3.Text
            import androidx.compose.ui.window.Dialog
            val a = Modifier.padding(12.dp)
            val b = Color(0xFF4CAF50)
            val c = TextStyle(fontSize = size)
            val d = RoundedCornerShape(radius)
            val e = tint.copy(alpha = 0.42f)
            androidx.compose.material3.Text("qualified")
            val f = KatariTheme.colors.primary
            val g = Modifier.clip(KatariTheme.shapes.large)
            val h = Modifier.shadow(elevation)
            val ok = Modifier.padding(0.dp).background(Color.Transparent).alpha(progress)
            val drawn = KatariTheme.chart.track
            """.trimIndent(),
        )

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), findings.map { it.lineNumber })
    }

    @Test
    fun `the kit, generated icons, tests, comments and strings are not checked`() {
        val violation = "import androidx.compose.ui.unit.dp\nval a = 12.dp"
        val findings = check("presentation-core/src/main/java/Kit.kt", violation) +
            check("icons/material-symbols/src/main/java/Icon.kt", violation) +
            check("app/src/test/java/ScreenTest.kt", violation) +
            check(
                "app/src/main/java/Screen.kt",
                "import androidx.compose.ui.unit.dp\n// 12.dp\nval label = \"12.dp\"\n/* Color(0) */",
            )

        assertEquals(emptyList<UiKitBoundaryFinding>(), findings)
    }

    @Test
    fun `only presentation-core may depend on or opt into Compose Material`() {
        val dependency = listOf(
            "implementation(libs.androidx.compose.material3)",
            "\"-opt-in=androidx.compose.material3.ExperimentalMaterial3Api\",",
        ).joinToString("\n")
        val findings = checkUiKitBoundaries(
            kotlinSources = emptyList(),
            buildScripts = listOf(
                UiKitBoundarySource("presentation-core/build.gradle.kts", dependency),
                UiKitBoundarySource("translation/ui/build.gradle.kts", dependency),
            ),
        )

        assertEquals(listOf(1, 2), findings.map { it.lineNumber })
        assertEquals(setOf("translation/ui/build.gradle.kts"), findings.map { it.relativePath }.toSet())
    }

    @Test
    fun `window size and orientation reads outside the kit are reported, other configuration reads are not`() {
        val findings = check(
            "app/src/main/java/Screen.kt",
            """
            import androidx.compose.ui.platform.LocalConfiguration
            val a = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val config = LocalConfiguration.current
            val b = config.screenWidthDp
            val c = context.resources.configuration?.smallestScreenWidthDp
            val d = LocalWindowInfo.current.containerSize
            val e = resources.displayMetrics.heightPixels
            val f = WindowInfoTracker.getOrCreate(context)
            val locale = config.locales[0]
            val night = LocalConfiguration.current.uiMode
            val text = region.orientation
            """.trimIndent(),
        )

        assertEquals(listOf(2, 4, 5, 6, 7, 8), findings.map { it.lineNumber })
        assertTrue(findings.all { it.reason.endsWith("see .agents/skills/adaptive-layout") })
    }

    @Test
    fun `raw vertical lazy lists outside the kit are reported`() {
        val findings = check(
            "app/src/main/java/Screen.kt",
            """
            import androidx.compose.foundation.lazy.LazyColumn
            LazyColumn(state = state) { }
            LazyColumn { }
            ScrollbarLazyColumn(contentPadding = padding) { }
            FastScrollLazyColumn { }
            RowList(fastScroll = true) { }
            """.trimIndent(),
        )

        assertEquals(listOf(2, 3, 4, 5), findings.map { it.lineNumber })
    }

    @Test
    fun `grid cells built outside the kit are reported`() {
        val findings = check(
            "app/src/main/java/Screen.kt",
            """
            import androidx.compose.foundation.lazy.grid.GridCells
            val a = GridCells.Fixed(1)
            val b = GridCells.Adaptive(minSize)
            val c = StaggeredGridCells.FixedSize(size)
            val covers = coverGridCells(coverSize)
            val days = SimpleGridCells.Fixed(7)
            """.trimIndent(),
        )

        assertEquals(listOf(2, 3, 4), findings.map { it.lineNumber })
    }

    @Test
    fun `readers and the player are exempt from the adaptive layout rules only`() {
        val findings = check(
            "entry-interactions/anime/src/main/java/eu/kanade/tachiyomi/ui/video/player/components/Drawer.kt",
            """
            import androidx.compose.foundation.lazy.LazyColumn
            val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            LazyColumn(columns = GridCells.Fixed(2)) { }
            val gap = 12.dp
            """.trimIndent(),
        )

        assertEquals(listOf(4), findings.map { it.lineNumber })
    }

    private fun check(path: String, content: String): List<UiKitBoundaryFinding> {
        return checkUiKitBoundaries(listOf(UiKitBoundarySource(path, content)), emptyList())
    }
}
