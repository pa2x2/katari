package mihon.entry.interactions.download

/** Downloads need the internet, and a network that counts as Wi-Fi while they are limited to Wi-Fi. */
internal fun isEntryDownloadNetworkAllowed(isOnline: Boolean, countsAsWifi: Boolean, requireWifi: Boolean): Boolean =
    isOnline && (!requireWifi || countsAsWifi)
