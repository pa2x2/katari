package eu.kanade.tachiyomi.extension.model

enum class InstallStep {
    Idle,
    Pending,
    Downloading,
    Installing,
    Installed,
    RequiresUserAction,
    Error,
    ;

    fun isCompleted(): Boolean {
        return this == Installed || this == RequiresUserAction || this == Error || this == Idle
    }
}
