package app.katari.sync.client

/** Rows that sync, for a device to hold before it joins: [episodes] episodes of one show, the first ones read. */
internal fun showRows(url: String, episodes: Int = 1, read: Int = 0, profileId: Long = 1): Array<String> = arrayOf(
    "INSERT OR IGNORE INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
    """
    INSERT INTO entries(profile_id, source, url, title, favorite, type)
    VALUES ($profileId, -2, '$url', 'Show', 1, 'anime')
    """,
    *(1..episodes).map { number ->
        """
        INSERT INTO chapters(entry_id, url, name, chapter_number, read)
        SELECT _id, '$url/$number', 'Episode $number', $number, ${if (number <= read) 1 else 0}
        FROM entries WHERE url = '$url'
        """
    }.toTypedArray(),
)
