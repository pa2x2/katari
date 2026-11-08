/**
 * Prints the matrix of the Showcase workflow's capture jobs: one job for each group of scenes.ts, and whether it records
 * the recap clip, which needs ffmpeg. The workflow takes it from here rather than naming the groups itself, since a group
 * it missed would capture nothing, and the comparison would then retire every capture of that group from the site.
 */
import { RECAP_SCENE, SCENE_GROUPS } from "./scenes.ts";

const include = Object.entries(SCENE_GROUPS).map(([group, scenes]) => ({
  group,
  "records-clip": scenes.includes(RECAP_SCENE),
}));
console.log(JSON.stringify({ include }));
