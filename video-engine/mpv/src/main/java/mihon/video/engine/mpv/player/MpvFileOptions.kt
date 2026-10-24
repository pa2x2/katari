package mihon.video.engine.mpv.player

/*
 * A source's mpv options for a stream go with its loadfile command, so mpv drops them when that file ends. loadfile
 * reads them as one comma-separated list; each value is length-prefixed (%n%, in bytes) so commas, equals signs and
 * quotes inside it stay part of it.
 */

/** [options] as loadfile's options argument, leaving out names mpv couldn't parse; null when none remain. */
internal fun loadfileOptions(options: Map<String, String>): String? =
    options.entries
        .filter { (name, _) -> OPTION_NAME.matches(name) }
        .joinToString(separator = ",") { (name, value) -> "$name=%${value.encodeToByteArray().size}%$value" }
        .ifEmpty { null }

private val OPTION_NAME = Regex("[a-z0-9][a-z0-9-]*")
