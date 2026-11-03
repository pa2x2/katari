package mihon.aniyomi.runtime

/** Thrown by the torrent calls Aniyomi extensions make; Katari has no torrent engine. */
class TorrentUnsupportedException : UnsupportedOperationException("Katari doesn't play torrents")
