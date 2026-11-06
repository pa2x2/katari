package app.katari.sync.ui.question

/**
 * What each answer of a migration question names, so no two answers read the same (D063): the target's source while
 * the sources differ, else the device that made each migration, else which migration came first.
 */
internal sealed interface KeepLabel {
    data class Source(val name: String) : KeepLabel

    data class Device(val name: String) : KeepLabel

    data class Order(val later: Boolean) : KeepLabel
}

/** One label per target, in [NamedQuestion.Migration.targets]' order; [entryTitle] stands in for an unknown source. */
internal fun NamedQuestion.Migration.keepLabels(entryTitle: String): List<KeepLabel> {
    val sources = targets.map { it.source ?: entryTitle }
    if (sources.distinct().size == sources.size) return sources.map(KeepLabel::Source)
    val devices = targets.map { it.device }
    if (devices.none { it == null } && devices.distinct().size == devices.size) {
        return devices.map { KeepLabel.Device(checkNotNull(it)) }
    }
    return targets.indices.map { KeepLabel.Order(later = it == targets.lastIndex) }
}
