package tachiyomi.domain.statistics.service

object StatisticsActivityPolicy {
    /** Minimum accumulated active duration for a media session to contribute timed Statistics activity. */
    const val MINIMUM_SESSION_DURATION_MILLIS = 10_000L
}
