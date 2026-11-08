package app.katari.android.di

import android.content.Context
import app.katari.core.metro.metroGraph

val Context.appGraph get() = metroGraph<AppGraph>()
