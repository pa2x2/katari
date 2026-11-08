package app.katari.profiles.core

import tachiyomi.data.ActiveProfileProvider

interface ProfileAwareStore : ProfileStore, ActiveProfileProvider
