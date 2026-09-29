import com.android.build.api.dsl.SettingsExtension
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

private const val SEPARATE_R8_PROCESS_PROFILE = "separateR8Process"

/**
 * Runs R8 in its own JVM. Shrinking the app keeps about 3.8 GB live, which fills the Gradle daemon
 * heap by itself and makes any task running beside it fail with `Java heap space`.
 */
@Suppress("UNUSED")
class PluginAndroidSettings : Plugin<Settings> {
    override fun apply(target: Settings): Unit = with(target) {
        pluginManager.apply("com.android.settings")
        extensions.getByType(SettingsExtension::class.java).execution {
            profiles {
                create(SEPARATE_R8_PROCESS_PROFILE) {
                    r8 {
                        runInSeparateProcess = true
                        jvmOptions += "-Xmx6g"
                    }
                }
            }
            defaultProfile = SEPARATE_R8_PROCESS_PROFILE
        }
    }
}
