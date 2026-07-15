package mihon.entry.interactions.book

import androidx.lifecycle.ViewModel
import mihon.book.api.BookLocator

/** Retains an opened BOOK session and its latest in-memory position across configuration changes. */
internal class BookReaderSessionViewModel : ViewModel() {
    var session: OpenedBookReaderSession? = null
        private set

    var currentLocator: BookLocator? = null
        private set

    fun attach(session: OpenedBookReaderSession) {
        check(this.session == null) { "A BOOK reader session is already attached" }
        this.session = session
        currentLocator = session.initialLocator
    }

    fun updateLocation(locator: BookLocator) {
        currentLocator = locator
    }

    fun release() {
        session?.close()
        session = null
        currentLocator = null
    }

    override fun onCleared() {
        release()
    }
}
