package eu.kanade.tachiyomi.extension.model

sealed interface InstallStep {
    data object Idle : InstallStep
    data object Pending : InstallStep
    data object Downloading : InstallStep
    data object Installing : InstallStep

    /** Its code is being converted to run on a platform that can't run an apk's directly. */
    data object Converting : InstallStep
    data object Installed : InstallStep
    data object RequiresUserAction : InstallStep

    /**
     * [message] says what went wrong; [stackTrace] is there when an exception did.
     */
    data class Error(val message: String, val stackTrace: String? = null) : InstallStep {
        companion object {
            fun from(e: Throwable): Error {
                val root = generateSequence(e) { it.cause }.last()
                return Error(
                    message = root.message ?: root::class.simpleName.orEmpty(),
                    stackTrace = e.stackTraceToString(),
                )
            }
        }
    }

    fun isCompleted(): Boolean {
        return this == Installed || this == RequiresUserAction || this is Error || this == Idle
    }
}
