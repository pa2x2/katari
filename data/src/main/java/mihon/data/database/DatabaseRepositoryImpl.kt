package mihon.data.database

import kotlinx.coroutines.flow.MutableStateFlow
import mihon.domain.database.repository.DatabaseRepository

class DatabaseRepositoryImpl : DatabaseRepository {

    override val isMigrating = MutableStateFlow(false)
}
