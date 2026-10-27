package tachiyomi.i18n

// TODO: janky workaround for https://github.com/icerockdev/moko-resources/issues/337
internal fun String.unescapeQuotes() =
    this.replace("""\""", """"""")
