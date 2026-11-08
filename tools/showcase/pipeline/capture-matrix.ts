/**
 * Prints the matrix of the Showcase workflow's capture jobs: one job for each group of scenes.ts, with the devices it
 * runs scenes on and whether it runs the sync server, which decide the builds and tools the job sets up, and whether it
 * records the recap clip, which needs ffmpeg. The workflow takes it from here rather than naming the groups itself,
 * since a group it missed would capture nothing, and the comparison would then retire every capture of that group from
 * the site.
 */
import { platformsOf, RECAP_SCENE, SCENE_GROUPS } from "./scenes.ts";

const include = Object.entries(SCENE_GROUPS).map(([group, { scenes, syncs }]) => {
  const platforms = platformsOf(scenes);
  return {
    group,
    phone: platforms.has("phone"),
    desktop: platforms.has("desktop"),
    browser: platforms.has("browser"),
    syncs: syncs === true,
    "records-clip": scenes.some((scene) => scene.platform === "phone" && scene.name === RECAP_SCENE),
  };
});
console.log(JSON.stringify({ include }));
