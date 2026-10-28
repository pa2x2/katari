import mihon.gradle.getAppVersionName
import mihon.gradle.getLatestCommitSha
import mihon.gradle.tasks.GenerateDesktopBuildInfoTask

plugins {
    alias(mihonx.plugins.desktop.application)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

val generateDesktopBuildInfo = tasks.register<GenerateDesktopBuildInfoTask>("generateDesktopBuildInfo") {
    packageName.set("mihon.desktop.application")
    versionName.set(getAppVersionName())
    commitSha.set(getLatestCommitSha())
    outputDir.set(layout.buildDirectory.dir("generated/desktopBuildInfo"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generateDesktopBuildInfo.flatMap { it.outputDir })
    }
}

dependencies {
    implementation(projects.appUi)
    implementation(projects.backup)
    implementation(projects.presentationCore)
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.entryInteractions)
    implementation(projects.entryInteractions.api)
    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.core.platform)
    implementation(projects.i18n)
    implementation(projects.trackers)

    implementation(libs.metrox.viewmodel.compose)
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.protobuf)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.desktop.application {
    mainClass = "mihon.desktop.MainKt"
}
