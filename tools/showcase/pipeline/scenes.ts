/**
 * The scenes, in groups that each run on freshly set up devices of their own, so CI runs the groups side by side. No
 * scene may depend on what a scene of another group did; within a group they run in this order. A scene runs on the
 * phone, as a UiAutomator class of tools/showcase/capture against the Android app on an emulator, or on the desktop, as
 * a class of tools/showcase/desktop-capture inside the desktop app.
 *
 * Opening a reader on the phone records a reading session at the showcase moment, so every phone scene that shows
 * reading history comes before the first one of its group that opens a reader. The groups are balanced by how long they
 * take on a CI runner, where the recap alone takes about five minutes.
 */
export type Platform = "phone" | "desktop";

export interface Scene {
  platform: Platform;
  /** The scene's class under its capture module's package, such as `reader.BookScenes`. */
  name: string;
}

/**
 * The phone scene that records the recap clip, which the pipeline re-encodes for the site with ffmpeg, and leaves the
 * image the recap shares in the app's cache.
 */
export const RECAP_SCENE = "statistics.RecapScenes";

export const SCENE_GROUPS: Record<string, Scene[]> = {
  recap: phone(RECAP_SCENE),
  translation: phone(
    "translation.TranslatorScenes",
    "translation.TranslationSettingsScenes",
    "translation.TranslationScenes",
  ),
  "library-and-statistics": phone("library.LibraryScenes", "library.EntryScenes", "statistics.StatisticsScenes"),
  "browse-and-backup": phone("browse.BrowseScenes", "settings.BackupScenes"),
  readers: phone("reader.BookScenes", "reader.ComicScenes", "reader.PlayerScenes"),
  desktop: desktop("reader.BookScenes", "reader.ComicScenes", "reader.PlayerScenes"),
};

/** How `--scenes` names a scene: its platform and class, such as `phone:reader.BookScenes`. */
export function sceneId(scene: Scene): string {
  return `${scene.platform}:${scene.name}`;
}

export const SCENES = Object.values(SCENE_GROUPS).flat();

/** The phone scenes that translate with Offline Translator, which setup then installs and readies on-page translation for. */
export const TRANSLATION_SCENES = names(SCENE_GROUPS.translation!);

/**
 * The phone scenes that find the Personal profile's year recap opened. Until it is, More carries a mark for it, which
 * the library's stills show beside the recap the statistics dashboard offers, and the rest of the showcase leaves out.
 */
export const YEAR_RECAP_OPENED_SCENES = names([
  ...SCENE_GROUPS.translation!,
  ...SCENE_GROUPS["browse-and-backup"]!,
  ...SCENE_GROUPS.readers!,
]);

function phone(...names: string[]): Scene[] {
  return names.map((name) => ({ platform: "phone", name }));
}

function desktop(...names: string[]): Scene[] {
  return names.map((name) => ({ platform: "desktop", name }));
}

function names(scenes: Scene[]): string[] {
  return scenes.map((scene) => scene.name);
}
