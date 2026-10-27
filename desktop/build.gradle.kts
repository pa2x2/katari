plugins {
    alias(mihonx.plugins.desktop.application)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

dependencies {
    implementation(projects.appUi)
    implementation(projects.presentationCore)
    implementation(projects.domain)
    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.core.platform)
    implementation(projects.i18n)

    implementation(libs.metrox.viewmodel.compose)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.desktop.application {
    mainClass = "mihon.desktop.MainKt"
}
