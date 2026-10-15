package mihon.gradle.extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForMihonx
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

internal val Project.libs get() = the<LibrariesForLibs>()
internal val Project.mihonx get() = the<LibrariesForMihonx>()

internal fun Project.plugins(block: PluginManager.() -> Unit) {
    pluginManager.apply(block)
}

internal fun Project.android(block: CommonExtension.() -> Unit) {
    extensions.configure(block)
}

fun Project.configureTest() {
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
        // mockk's inline mocking needs byte-buddy's agent and appends its dispatcher to the boot class path.
        // Attached at startup, the agent is reused instead of being attached to the running JVM, which JDK 21
        // warns about and later JDKs refuse. Class-data sharing is off because that boot class path append
        // leaves it covering only the JDK's own classes.
        doFirst {
            this as Test
            classpath.find { it.name.startsWith("byte-buddy-agent-") }
                ?.let { jvmArgs("-javaagent:${it.absolutePath}", "-Xshare:off") }
        }
    }
}
