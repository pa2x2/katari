plugins {
    alias(mihonx.plugins.desktop.application)
    alias(mihonx.plugins.spotless)
}

dependencies {
    implementation(projects.presentationCore)
    implementation(projects.domain)
    implementation(projects.core.common)
    implementation(projects.i18n)
}

compose.desktop.application {
    mainClass = "mihon.desktop.MainKt"
}
