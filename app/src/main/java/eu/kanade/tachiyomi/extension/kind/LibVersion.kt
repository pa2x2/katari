package eu.kanade.tachiyomi.extension.kind

/** An extension library version, `major.minor` with an optional patch, as Tachiyomi and Katari extensions declare it. */
internal data class LibVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0,
) : Comparable<LibVersion> {

    override fun compareTo(other: LibVersion): Int {
        return compareValuesBy(this, other, LibVersion::major, LibVersion::minor, LibVersion::patch)
    }

    override fun toString(): String {
        return if (patch == 0) {
            "$major.$minor"
        } else {
            "$major.$minor.$patch"
        }
    }

    fun withoutPatch(): LibVersion = copy(patch = 0)

    fun toDouble(): Double {
        return "$major.$minor".toDouble()
    }

    companion object {
        fun parse(value: String): LibVersion? {
            val parts = value.split('.')
            if (parts.size !in 2..3) return null

            return LibVersion(
                major = parts[0].toIntOrNull() ?: return null,
                minor = parts[1].toIntOrNull() ?: return null,
                patch = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }

        fun range(start: String, endInclusive: String): ClosedRange<LibVersion> {
            return parse(start)!!..parse(endInclusive)!!
        }
    }
}

/** Orders two extensions' version names by the library they declare, `null` when either declares none. */
internal fun compareLibVersions(firstVersionName: String, secondVersionName: String): Int? {
    val first = parseVersionNameAsLibVersion(firstVersionName) ?: return null
    val second = parseVersionNameAsLibVersion(secondVersionName) ?: return null
    return first.compareTo(second)
}

private fun parseVersionNameAsLibVersion(versionName: String): LibVersion? {
    return LibVersion.parse(versionName.substringBeforeLast('.'))
        ?: LibVersion.parse(versionName)
}

/** The library a manifest's extension library entry declares, whatever type the build wrote it as. */
internal fun extensionLibValue(value: Any?): String? {
    return when (value) {
        is Int -> value.takeUnless { it == 0 }?.toString()
        is Float -> value.takeUnless { it == 0.0f }?.toString()
        is Double -> value.takeUnless { it == 0.0 }?.toString()
        is String -> value.takeUnless { it.isBlank() || it == "0" || it == "0.0" }
        else -> null
    }
}
