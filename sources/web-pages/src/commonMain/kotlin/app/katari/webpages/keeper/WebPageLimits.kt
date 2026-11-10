package app.katari.webpages.keeper

/**
 * How many pages a platform keeps open at once, each costing the memory of a browser tab.
 *
 * @property perSource for one source, under different names.
 * @property total for all sources together.
 */
class WebPageLimits(val perSource: Int, val total: Int)
