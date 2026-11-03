package eu.kanade.tachiyomi.util.system

/** Android's texture limits. Desktop has no EGL to ask, so the device limit is the safe one. */
object GLUtil {
    val DEVICE_TEXTURE_LIMIT: Int = SAFE_TEXTURE_LIMIT

    const val SAFE_TEXTURE_LIMIT: Int = 2048

    val CUSTOM_TEXTURE_LIMIT_OPTIONS: List<Int> by lazy {
        val steps = DEVICE_TEXTURE_LIMIT / MULTIPLIER
        buildList(steps) {
            add(DEVICE_TEXTURE_LIMIT)
            for (step in steps downTo 2) {
                val value = step * MULTIPLIER
                if (value >= DEVICE_TEXTURE_LIMIT) continue
                add(value)
            }
        }
    }
}

private const val MULTIPLIER: Int = 1024
