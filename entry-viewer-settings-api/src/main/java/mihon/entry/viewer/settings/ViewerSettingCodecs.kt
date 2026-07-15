package mihon.entry.viewer.settings

object ViewerSettingCodecs {
    val Boolean: ViewerSettingCodec<kotlin.Boolean> = codec(
        encode = kotlin.Boolean::toString,
        decode = { value -> value.toBooleanStrictOrNull() },
    )

    val Int: ViewerSettingCodec<kotlin.Int> = codec(
        encode = kotlin.Int::toString,
        decode = { value -> value.toIntOrNull() },
    )

    val Long: ViewerSettingCodec<kotlin.Long> = codec(
        encode = kotlin.Long::toString,
        decode = { value -> value.toLongOrNull() },
    )

    val Double: ViewerSettingCodec<kotlin.Double> = codec(
        encode = kotlin.Double::toString,
        decode = { value -> value.toDoubleOrNull()?.takeIf(kotlin.Double::isFinite) },
    )

    val String: ViewerSettingCodec<kotlin.String> = codec(
        encode = { it },
        decode = { it },
    )

    fun <T> codec(
        encode: (T) -> kotlin.String,
        decode: (kotlin.String) -> T?,
    ): ViewerSettingCodec<T> = object : ViewerSettingCodec<T> {
        override fun encode(value: T): kotlin.String = encode.invoke(value)
        override fun decode(value: kotlin.String): T? = decode.invoke(value)
    }
}
