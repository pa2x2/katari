plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

android {
    namespace = "eu.kanade.tachiyomi.source.compat"
}

// Host tests can't load the AAR's Android libraries; the JVM jar carries the same engine for desktop platforms.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module(libs.quickjs.kt.android.get().module.toString()))
            .using(module(libs.quickjs.kt.jvm.get().toString()))
            .withoutArtifactSelectors()
    }
}

dependencies {
    api(projects.entrySourceApi)
    api(projects.sourceApi)
    implementation(projects.aniyomiSourceApi)
    implementation(projects.scriptRuntime)

    // Released without Gradle metadata, so the AAR is named explicitly
    implementation(libs.quickjs.kt.android) {
        artifact { type = "aar" }
    }

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    testRuntimeOnly(libs.junit.platform.launcher)
}
