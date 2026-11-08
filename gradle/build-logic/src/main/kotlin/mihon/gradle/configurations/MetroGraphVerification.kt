package mihon.gradle.configurations

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import mihon.gradle.extensions.libs
import org.gradle.api.Project

/**
 * Makes `verifyTests` run Metro's hidden-dependency check on every variant of this Android application, which builds the
 * app's graph.
 *
 * Metro builds a graph from the contributions it finds on the graph's compile classpath. A contribution that a
 * dependency brings in only through its own `implementation` is left out, and joins the graph unannounced once that
 * dependency or Metro changes.
 */
fun Project.verifyAndroidMetroGraph() {
    pluginManager.withPlugin(libs.plugins.metro.get().pluginId) {
        extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
            tasks.named(VERIFY_TESTS_TASK_NAME) { dependsOn(hiddenDependencyCheck(variant.name)) }
        }
    }
}

/** As [verifyAndroidMetroGraph], for the `main` compilation of a desktop application. */
fun Project.verifyDesktopMetroGraph() {
    pluginManager.withPlugin(libs.plugins.metro.get().pluginId) {
        tasks.named(VERIFY_TESTS_TASK_NAME) { dependsOn(hiddenDependencyCheck("main")) }
    }
}

private fun hiddenDependencyCheck(compilation: String): String =
    "check${compilation.replaceFirstChar(Char::uppercase)}MetroHiddenDependencies"
