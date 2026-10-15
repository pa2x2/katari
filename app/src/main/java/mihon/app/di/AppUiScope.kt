package mihon.app.di

/**
 * Contributions only the application graph composes: the view models of the app's own screens. The Feature validation
 * graphs aggregate [dev.zacsweers.metro.AppScope] alone, so they validate module contributions without having to supply
 * everything the app's screens depend on.
 */
abstract class AppUiScope private constructor()
