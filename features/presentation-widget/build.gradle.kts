plugins {
    alias(katari.plugins.android.library)
    alias(katari.plugins.compose)

    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
}

android {
    namespace = "tachiyomi.presentation.widget"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.core.domain)
    implementation(projects.core.presentationCore)
    api(projects.core.i18n)

    implementation(libs.androidx.glance.appWidget)
    implementation(libs.material)

    implementation(libs.kotlinx.datetime)

    implementation(libs.coil.core)
}
