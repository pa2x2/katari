/**
 * The moment every screenshot shows. The pipeline sets the emulator's clock to it, and the seed's history is written
 * relative to it, so statistics, recaps, and relative dates read the same on every run.
 *
 * Mid-December: the year's recap is already offered, and November's monthly recap is the latest one.
 */
export const SHOWCASE_NOW = Date.UTC(2026, 11, 15, 19, 30);

export const SHOWCASE_TIME_ZONE = "UTC";

/** Statistics treat activity before this as untracked, so the year shown starts here. */
export const STATISTICS_EPOCH = Date.UTC(2025, 11, 1);
