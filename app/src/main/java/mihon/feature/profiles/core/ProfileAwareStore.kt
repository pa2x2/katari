package mihon.feature.profiles.core

import tachiyomi.data.ActiveProfileProvider

interface ProfileAwareStore : ProfileStore, ActiveProfileProvider
