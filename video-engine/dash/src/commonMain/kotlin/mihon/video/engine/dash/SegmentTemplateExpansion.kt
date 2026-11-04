package mihon.video.engine.dash

/**
 * [template] with its identifiers replaced: `$RepresentationID$`, `$Bandwidth$`, `$Number$` and `$Time$`, each with
 * an optional `%0<width>d` format, and `$$` for a dollar sign. [number] and [time] are null where the template names
 * an initialization segment, which has neither.
 */
internal fun expandSegmentTemplate(
    template: String,
    representation: DashRepresentation,
    number: Long?,
    time: Long?,
): String = IDENTIFIER.replace(template) { match ->
    val (name, width) = match.destructured
    val value: Any = when (name) {
        "" -> return@replace "$"
        "RepresentationID" -> return@replace representation.id
        "Bandwidth" -> representation.bandwidth
        "Number" -> number
        "Time" -> time
        else -> null
    } ?: throw UnsupportedDashManifestException("Template identifier '${match.value}' in '$template'")
    value.toString().padStart(width.toIntOrNull() ?: 0, '0')
}

private val IDENTIFIER = Regex("""\$(\w*)(?:%0(\d+)d)?\$""")
