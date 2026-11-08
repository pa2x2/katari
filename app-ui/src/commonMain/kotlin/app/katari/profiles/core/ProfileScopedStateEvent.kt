package app.katari.profiles.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface ProfileScopedStateEvent<out T> {
    val profileId: Long

    data class Reset(
        override val profileId: Long,
    ) : ProfileScopedStateEvent<Nothing>

    data class Value<T>(
        override val profileId: Long,
        val value: T,
    ) : ProfileScopedStateEvent<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> observeProfileScopedState(
    activeProfileIdFlow: Flow<Long>,
    observe: (Long) -> Flow<T>,
): Flow<ProfileScopedStateEvent<T>> {
    return activeProfileIdFlow
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            observe(profileId)
                .map<T, ProfileScopedStateEvent<T>> { value ->
                    ProfileScopedStateEvent.Value(profileId, value)
                }
                .onStart { emit(ProfileScopedStateEvent.Reset(profileId)) }
        }
}
