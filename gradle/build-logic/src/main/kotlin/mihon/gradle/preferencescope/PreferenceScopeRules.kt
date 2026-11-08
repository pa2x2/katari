package mihon.gradle.preferencescope

/*
 * Sync sends a person's settings to their other devices and leaves the device's own alone, so every preference key
 * says which it is, in a `PreferenceScopeTable` of sync's client. A key nobody marked would silently stay on the
 * device, so a new setting, from this repository or merged from upstream, has to be marked before the build passes.
 * The marks live apart from the preference classes, most of which upstream owns, so a merge never conflicts on them.
 *
 * The check reads keys from the source alone (PreferenceKeyResolver). Code that asks a store for keys it doesn't own,
 * such as backup restore or queues kept in files of their own, is listed in PREFERENCE_SCOPE_EXEMPT_FILES.
 */

internal data class PreferenceScopeSource(val relativePath: String, val content: String)

internal data class PreferenceScopeFinding(val relativePath: String, val lineNumber: Int, val reason: String)

/** [exemptFiles] are the paths of files that only ask for keys other code owns, with why. */
internal fun checkPreferenceScopes(
    sources: List<PreferenceScopeSource>,
    exemptFiles: Map<String, String> = emptyMap(),
): List<PreferenceScopeFinding> {
    val files = sources.filter { it.relativePath.isMainSource() }.map { KotlinSourceText(it.relativePath, it.content) }
    val paths = files.map { it.relativePath }.toSet()
    val marks = preferenceScopeMarks(files)
    val requests = preferenceKeyRequests(
        files.filterNot { it.relativePath in exemptFiles || PREFERENCE_STORES in it.relativePath },
    )
    val used = mutableSetOf<PreferenceScopeMark>()

    val unmarked = requests.flatMap { request ->
        val keys = request.keys ?: return@flatMap listOf(
            request.finding(
                "the preference key ${request.expression} can't be read from the source; pass a string literal, a " +
                    "constant or a key family's key(…), or list this file in PreferenceScopeRules if it only " +
                    "reads keys other code owns",
            ),
        )
        keys.mapNotNull { key ->
            val mark = marks.marks.markOf(key)
            if (mark != null) used += mark
            when {
                mark != null || key is ResolvedKey.DeviceByDefinition -> null
                key is ResolvedKey.Exact -> request.finding(
                    "the preference key \"${key.key}\" isn't marked person or device; mark it in a " +
                        "PreferenceScopeTable under $SCOPE_TABLES",
                )
                key is ResolvedKey.Family -> request.finding(
                    "the preference keys starting with \"${key.prefix}\" aren't marked person or device; mark the " +
                        "prefix with personPrefix or devicePrefix under $SCOPE_TABLES",
                )
                else -> null
            }
        }
    }
    val duplicates = marks.marks.groupBy { it.value to it.isPrefix }.values.filter { it.size > 1 }.flatMap { same ->
        same.drop(1).map { PreferenceScopeFinding(it.relativePath, it.lineNumber, "\"${it.value}\" is marked twice") }
    }
    val stale = (marks.marks - used).map {
        PreferenceScopeFinding(it.relativePath, it.lineNumber, "no preference uses \"${it.value}\"; remove the mark")
    }
    val missingExemptions = exemptFiles.keys.filter { it !in paths }.map {
        PreferenceScopeFinding(it, 0, "is exempt from the preference scope check but no longer exists; remove it")
    }
    return marks.malformed + unmarked + duplicates + stale + missingExemptions
}

private fun List<PreferenceScopeMark>.markOf(key: ResolvedKey): PreferenceScopeMark? = when (key) {
    is ResolvedKey.Exact -> firstOrNull { !it.isPrefix && it.value == key.key }
        ?: filter { it.isPrefix && key.key.startsWith(it.value) }.maxByOrNull { it.value.length }
    is ResolvedKey.Family -> filter { it.isPrefix && key.prefix.startsWith(it.value) }.maxByOrNull { it.value.length }
    ResolvedKey.DeviceByDefinition -> null
}

private fun PreferenceKeyRequest.finding(reason: String) = PreferenceScopeFinding(relativePath, lineNumber, reason)

private fun String.isMainSource(): Boolean {
    if (!endsWith(".kt") || startsWith("gradle/")) return false
    val sourceSet = substringAfter("/src/", missingDelimiterValue = "").substringBefore("/")
    return sourceSet.isNotEmpty() && !sourceSet.contains("test", ignoreCase = true)
}

private const val SCOPE_TABLES = "features/sync/client/src/commonMain/kotlin/app/katari/sync/client/setting/scope"

/** The preference stores themselves, which pass on whatever key they are asked for. */
private const val PREFERENCE_STORES = "/kotlin/tachiyomi/core/common/preference/"

/** Code that asks stores for keys it doesn't own, with why. */
internal val PREFERENCE_SCOPE_EXEMPT_FILES = mapOf(
    "sources/extension-runtime/jvm-host/src/main/kotlin/mihon/extension/runtime/jvm/preferences/" +
        "FileSharedPreferences.kt" to
        "the desktop's shared preferences, a store",
    "features/backup/src/commonMain/kotlin/eu/kanade/tachiyomi/data/backup/restore/restorers/PreferenceRestorer.kt" to
        "restores whatever keys a backup holds",
    "features/sync/client/src/commonMain/kotlin/app/katari/sync/client/setting/apply/StoreSettingWrites.kt" to
        "writes the person-level keys other devices synced, which the scope tables already mark",
    "app/src/main/java/mihon/feature/profiles/core/AndroidLegacyProfilePreferenceKeys.kt" to
        "moves keys of earlier versions into profiles",
    "features/entry-interactions/src/commonMain/kotlin/mihon/entry/interactions/media/" +
        "DefaultEntryMediaCacheFeature.kt" to
        "reads the keys each cache declares through EntryMediaCacheAutoClearPreference",
    "app/src/main/java/eu/kanade/tachiyomi/data/library/AndroidLibraryUpdateWorkChain.kt" to
        "reads WorkManager input data, not preferences",
    "features/entry-interactions/manga/src/commonMain/kotlin/mihon/entry/interactions/manga/download/" +
        "DownloadStore.kt" to
        "the download queue, in a file of its own and keyed by download",
    "features/entry-interactions/manga/src/commonMain/kotlin/mihon/entry/interactions/manga/download/" +
        "DownloadPendingDeleter.kt" to
        "chapters waiting for deletion, in a file of their own and keyed by entry",
    "features/entry-interactions/anime/src/commonMain/kotlin/mihon/entry/interactions/anime/download/" +
        "AnimeDownloadStore.kt" to
        "the download queue, in a file of its own and keyed by download",
    "features/entry-interactions/book/src/commonMain/kotlin/mihon/entry/interactions/book/download/" +
        "BookDownloadQueueFileBackend.kt" to
        "the download queue of earlier versions, in a file of its own",
    "features/trackers/src/commonMain/kotlin/eu/kanade/domain/track/store/DelayedTrackingStore.kt" to
        "the tracking queue, in a file of its own and keyed by tracker binding",
)
