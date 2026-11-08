/**
 * The scenes, in groups that each run on freshly set up devices of their own, so CI runs the groups side by side. No
 * scene may depend on what a scene of another group did; within a group they run in this order. A scene runs on the
 * phone, as a UiAutomator class of tools/showcase/capture against the Android app on an emulator; on the desktop, as a
 * class of tools/showcase/desktop-capture inside the desktop app; or in a browser, as a page of browser/pages.ts.
 *
 * Opening a reader on the phone records a reading session at the showcase moment, so every phone scene that shows
 * reading history comes before the first one of its group that opens a reader. The groups are balanced by how long they
 * take on a CI runner, where the recap alone takes about five minutes.
 */
export type Platform = "phone" | "desktop" | "browser";

export interface Scene {
  platform: Platform;
  /** The scene's class under its capture module's package, such as `reader.BookScenes`, or a browser page's name. */
  name: string;
  /**
   * A desktop scene, by its class, that runs beside this phone scene: it starts once the phone scene signals that it is
   * ready for it, and ends after the phone scene. The hand-off scene's reader waits for the progress the desktop makes.
   */
  alongside?: string;
}

export interface SceneGroup {
  scenes: Scene[];
  /**
   * Whether its devices sync, through a sync server the group runs (sync/sync-server.ts). Its desktop then starts empty
   * and joins the phone's account in a scene, rather than restoring the seed.
   */
  syncs?: boolean;
}

/**
 * The phone scene that records the recap clip, which the pipeline re-encodes for the site with ffmpeg, and leaves the
 * image the recap shares in the app's cache.
 */
export const RECAP_SCENE = "statistics.RecapScenes";

export const SCENE_GROUPS: Record<string, SceneGroup> = {
  recap: { scenes: phone(RECAP_SCENE) },
  translation: {
    scenes: phone("translation.TranslatorScenes", "translation.TranslationSettingsScenes", "translation.TranslationScenes"),
  },
  "library-and-statistics": {
    scenes: phone("library.LibraryScenes", "library.EntryScenes", "statistics.StatisticsScenes"),
  },
  "browse-and-backup": { scenes: phone("browse.BrowseScenes", "settings.BackupScenes") },
  readers: { scenes: phone("reader.BookScenes", "reader.ComicScenes", "reader.PlayerScenes") },
  desktop: { scenes: desktop("reader.BookScenes", "reader.ComicScenes", "reader.PlayerScenes") },
  // The phone signs in with its seeded library, the desktop joins and takes it in, and the phone's reader then hears
  // of the desktop reading on.
  sync: {
    syncs: true,
    scenes: [
      ...phone("sync.SignInScenes"),
      ...desktop("sync.JoinScenes", "sync.LibraryScenes"),
      ...phone("sync.SyncPageScenes", "sync.LinkCodeScenes"),
      { platform: "phone", name: "sync.HandOffScenes", alongside: "sync.ReadAheadScenes" },
      { platform: "browser", name: "sync-server" },
    ],
  },
};

/** How `--scenes` names a scene: its platform and class, such as `phone:reader.BookScenes`. */
export function sceneId(scene: Scene): string {
  return `${scene.platform}:${scene.name}`;
}

export const SCENES = Object.values(SCENE_GROUPS).flatMap((group) => group.scenes);

/** Every platform [scenes] run on, those started alongside others included. */
export function platformsOf(scenes: Scene[]): Set<Platform> {
  return new Set(scenes.flatMap((scene): Platform[] => (scene.alongside ? [scene.platform, "desktop"] : [scene.platform])));
}

/** The phone scenes that translate with Offline Translator, which setup then installs and readies on-page translation for. */
export const TRANSLATION_SCENES = names(SCENE_GROUPS.translation!.scenes);

/**
 * The phone scenes that find the Personal profile's year recap opened. Until it is, More carries a mark for it, which
 * the library's stills show beside the recap the statistics dashboard offers, and the rest of the showcase leaves out.
 */
export const YEAR_RECAP_OPENED_SCENES = names([
  ...SCENE_GROUPS.translation!.scenes,
  ...SCENE_GROUPS["browse-and-backup"]!.scenes,
  ...SCENE_GROUPS.readers!.scenes,
  ...SCENE_GROUPS.sync!.scenes,
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
