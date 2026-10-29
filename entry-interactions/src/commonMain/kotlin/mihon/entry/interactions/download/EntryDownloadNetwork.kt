package mihon.entry.interactions.download

/** Downloads need the internet, and a network that costs nothing extra while they are limited to Wi-Fi. */
internal fun isEntryDownloadNetworkAllowed(isOnline: Boolean, isUnmetered: Boolean, requireWifi: Boolean): Boolean =
    isOnline && (!requireWifi || isUnmetered)
