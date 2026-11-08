package xyz.nulldev.androidcompat.info

import android.content.pm.ApplicationInfo
import app.katari.extension.compat.AndroidCompatEnvironment

class ApplicationInfoImpl(
    environment: AndroidCompatEnvironment,
) : ApplicationInfo() {
    val debug: Boolean get() = false

    init {
        super.packageName = environment.packageName
        super.dataDir = environment.dataDir.absolutePath
    }
}
