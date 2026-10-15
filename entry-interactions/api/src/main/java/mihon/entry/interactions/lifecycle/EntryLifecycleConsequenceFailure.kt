package mihon.entry.interactions.lifecycle

/** A consequence that failed after its change had already committed; [consequence] names it for diagnostics. */
data class EntryLifecycleConsequenceFailure(
    val consequence: String,
    val cause: Throwable,
)
