package mihon.data.extension.model

/*
 * How indexes write the extension library an extension is built against. Katari's and Tachiyomi's have a minor
 * version ("1.6", "3.0"); Aniyomi's are whole numbers ("14", "16"), which is what tells their listings apart where the
 * index format itself doesn't.
 */

internal fun String.isAniyomiLib(): Boolean = toIntOrNull() != null

internal fun String.isTachiyomiLib(): Boolean {
    val parts = split('.')
    return parts.size >= 2 && parts.all { it.toIntOrNull() != null }
}

internal fun String.toLibVersionDouble(): Double {
    val parts = split('.')
    return if (parts.size >= 2) {
        "${parts[0]}.${parts[1]}".toDouble()
    } else {
        toDouble()
    }
}
