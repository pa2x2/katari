// Settings plugins are loaded by the settings classloader, which is the parent of every project's
// classloader. They live apart from the project plugins so that those keep resolving AGP and the
// generated catalog accessors from the project classpath.
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kotlin.samWithReceiver.get().pluginId)
    `java-gradle-plugin`
}

dependencies {
    implementation(libs.android.gradle.settings)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

gradlePlugin {
    plugins {
        register("android-settings") {
            id = mihonx.plugins.android.settings.get().pluginId
            implementationClass = "PluginAndroidSettings"
        }
    }
}
