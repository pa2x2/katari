package mihon.video.engine.dash

/**
 * Seconds in an `xs:duration` such as `PT12M14S` or `P0Y0M0DT0H9M54.00S`; null when [text] isn't one. Years and
 * months, which no presentation uses, count as 365 and 30 days.
 */
internal fun parseXsDuration(text: String): Double? {
    val match = XS_DURATION.matchEntire(text.trim()) ?: return null
    val (sign, years, months, days, hours, minutes, seconds) = match.destructured
    if (match.value.trimStart('-').let { it == "P" || it == "PT" || it.endsWith("T") }) return null
    fun part(value: String) = value.ifEmpty { "0" }.toDouble()
    val total = part(years) * 365 * DAY + part(months) * 30 * DAY + part(days) * DAY +
        part(hours) * 3_600 + part(minutes) * 60 + part(seconds)
    return if (sign == "-") -total else total
}

private const val DAY = 86_400.0

private val XS_DURATION =
    Regex("""(-?)P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d*)?)S)?)?""")
