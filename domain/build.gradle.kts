plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "tachiyomi.domain"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    api(projects.i18n)

    implementation(projects.entrySourceApi)
    implementation(projects.sourceApi)
    implementation(projects.core.common)

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.serialization)
    implementation(libs.stringSimilarity)

    implementation(libs.unifile)

    api(libs.sqldelight.androidxPaging)

    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.runtimeAnnotation)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
