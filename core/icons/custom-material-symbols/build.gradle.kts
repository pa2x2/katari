import app.katari.gradle.extensions.desktop
plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)

    alias(katari.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

kotlin {
    android {
        namespace = "app.katari.icons.custommaterialsymbols"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.compose.multiplatform.ui)
            }
        }
    }
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

valkyrie {
    packageName = "app.katari.icons.custommaterialsymbols"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "CustomMaterialSymbols"
        targetSourceSet = "commonMain"

        nested {
            name = "Rounded"
            sourceFolder = "rounded"
            autoMirror = false
        }
    }
}
