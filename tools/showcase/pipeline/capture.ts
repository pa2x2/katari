/**
 * Captures the showcase stills and clips, a group of scenes (scenes.ts) at a time, each group on devices set up afresh
 * for it: a wiped emulator for its phone scenes (device/phone-session.ts), a private display and desktop profile for
 * its desktop scenes (desktop/desktop-session.ts). CI runs each group in a job of its own, with --group; locally, the
 * groups run one after another.
 *
 * Expects the content server's folder (content/fetch.ts) and the seed, and for the phone the three APKs built:
 *
 *   ./gradlew :app:assembleBenchmark :tools:showcase:extension:assembleShowcase :tools:showcase:capture:assembleShowcase
 *
 * and for the desktop the AppDir, the capture agent and the extension, with libfaketime installed:
 *
 *   ./gradlew :desktop:assembleAppDir :tools:showcase:desktop-capture:jar :tools:showcase:extension:assembleShowcase
 */
import { type ChildProcess, spawn } from "node:child_process";
import { existsSync, mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import { setTimeout as sleep } from "node:timers/promises";
import { CONTENT_SERVER } from "../content/content-server.ts";
import { cacheDir } from "../content/download-cache.ts";
import { writeCredits } from "./credits.ts";
import { DesktopSession } from "./desktop/desktop-session.ts";
import { PhoneSession } from "./device/phone-session.ts";
import {
  type Platform,
  RECAP_SCENE,
  type Scene,
  SCENE_GROUPS,
  sceneId,
  SCENES,
  TRANSLATION_SCENES,
  YEAR_RECAP_OPENED_SCENES,
} from "./scenes.ts";
import { timed } from "./timed.ts";

const repository = resolve(import.meta.dirname, "..", "..", "..");
const { values } = parseArgs({
  options: {
    out: { type: "string", default: join(cacheDir(), "captures") },
    seed: { type: "string", default: join(cacheDir(), "showcase.tachibk") },
    served: { type: "string", default: join(cacheDir(), "served") },
    "app-apk": { type: "string", default: join(repository, "app/build/outputs/apk/benchmark/app-x86_64-benchmark.apk") },
    "extension-apk": {
      type: "string",
      default: join(repository, "tools/showcase/extension/build/outputs/apk/showcase/extension-showcase.apk"),
    },
    "capture-apk": {
      type: "string",
      default: join(repository, "tools/showcase/capture/build/outputs/apk/showcase/capture-showcase.apk"),
    },
    "desktop-app-dir": { type: "string", default: join(repository, "desktop/build/appimage/Katari-Dev.AppDir") },
    "desktop-agent": {
      type: "string",
      default: join(repository, "tools/showcase/desktop-capture/build/libs/desktop-capture.jar"),
    },
    // libfaketime.so.1, by default where Debian, Ubuntu, Arch and Fedora install it.
    faketime: { type: "string" },
    // Keeps the emulator of the last group running, for --reuse-emulator.
    "keep-emulator": { type: "boolean", default: false },
    // Runs only this group of scenes.ts, as each of CI's capture jobs does.
    group: { type: "string" },
    // For working on scenes: runs these, by their ids in scenes.ts (such as phone:reader.BookScenes; a bare class name
    // means it on every platform), on one set of devices in the order the groups list them. Scenes of different groups
    // then see what the others left, such as reading history.
    scenes: { type: "string" },
    // For working on scenes: uses the emulator a run with --keep-emulator left, already set up, and keeps it again.
    // Reader scenes it ran have added reading history, so history-dependent stills drift from a fresh run's.
    "reuse-emulator": { type: "boolean", default: false },
  },
});
const out = resolve(values.out);
const runs = chosenRuns();
const keepEmulator = values["keep-emulator"] || values["reuse-emulator"];
if (values["reuse-emulator"] && runs.length > 1) throw new Error("--reuse-emulator needs --group or --scenes");
const platforms = new Set(runs.flatMap((run) => run.scenes.map((scene) => scene.platform)));
const inputs = [values.seed, values.served, values["extension-apk"]];
if (platforms.has("phone")) inputs.push(values["app-apk"], values["capture-apk"]);
const faketime = platforms.has("desktop") ? faketimeLibrary() : "";
if (platforms.has("desktop")) inputs.push(values["desktop-app-dir"], values["desktop-agent"]);
for (const input of inputs) {
  if (!existsSync(input)) throw new Error(`${input} is missing; see the top of pipeline/capture.ts for how to build it.`);
}

rmSync(out, { recursive: true, force: true });
mkdirSync(out, { recursive: true });
const server = await startContentServer(values.served);
try {
  for (const [index, run] of runs.entries()) {
    await timed(`group ${run.name}`, () => capture(run.scenes, keepEmulator && index === runs.length - 1));
  }
  writeCredits(join(values.served, "catalog.json"), join(out, "credits.json"));
  console.log(out);
} finally {
  server.kill();
}

interface Run {
  name: string;
  scenes: Scene[];
}

/** The scenes to capture, a list for each set of devices in turn. */
function chosenRuns(): Run[] {
  if (values.group !== undefined && values.scenes !== undefined) throw new Error("Pass --group or --scenes, not both");
  if (values.scenes !== undefined) {
    const chosen = values.scenes.split(",");
    const scenes = SCENES.filter((scene) => chosen.includes(sceneId(scene)) || chosen.includes(scene.name));
    if (scenes.length === 0) throw new Error(`No scene in scenes.ts matches ${values.scenes}`);
    return [{ name: values.scenes, scenes }];
  }
  if (values.group !== undefined) {
    const scenes = SCENE_GROUPS[values.group];
    if (scenes === undefined) throw new Error(`No group in scenes.ts is called ${values.group}`);
    return [{ name: values.group, scenes }];
  }
  return Object.entries(SCENE_GROUPS).map(([name, scenes]) => ({ name, scenes }));
}

/** Captures [scenes] in order on devices set up for them, which it shuts down after, except a phone it [keep]s. */
async function capture(scenes: Scene[], keep: boolean): Promise<void> {
  const on = (platform: Platform) => scenes.filter((scene) => scene.platform === platform).map((scene) => scene.name);
  const phoneScenes = on("phone");
  const [phone, desktop] = await Promise.allSettled([
    phoneScenes.length === 0
      ? undefined
      : timed("setting up the phone", () =>
          PhoneSession.open(
            {
              appApk: values["app-apk"],
              extensionApk: values["extension-apk"],
              captureApk: values["capture-apk"],
              seed: values.seed,
              out,
              reuse: values["reuse-emulator"],
              keep,
            },
            {
              translates: phoneScenes.some((scene) => TRANSLATION_SCENES.includes(scene)),
              yearRecapOpened: YEAR_RECAP_OPENED_SCENES,
              recap: RECAP_SCENE,
            },
          ),
        ),
    on("desktop").length === 0
      ? undefined
      : timed("setting up the desktop", () =>
          DesktopSession.open({
            appDir: values["desktop-app-dir"],
            agentJar: values["desktop-agent"],
            extensionApk: values["extension-apk"],
            seed: values.seed,
            faketime,
            workDir: join(cacheDir(), "desktop"),
            out,
          }),
        ),
  ]);
  try {
    if (phone.status === "rejected") throw phone.reason;
    if (desktop.status === "rejected") throw desktop.reason;
    for (const scene of scenes) {
      await timed(`capturing ${sceneId(scene)}`, () =>
        scene.platform === "phone" ? phone.value!.run(scene.name) : desktop.value!.run(scene.name),
      );
    }
    phone.value?.collect();
  } catch (error) {
    if (phone.status === "fulfilled") phone.value?.saveDiagnostics();
    throw error;
  } finally {
    if (desktop.status === "fulfilled") desktop.value?.close();
    if (phone.status === "fulfilled") await phone.value?.close();
  }
}

function faketimeLibrary(): string {
  const candidates = [
    "/usr/lib/x86_64-linux-gnu/faketime/libfaketime.so.1",
    "/usr/lib/faketime/libfaketime.so.1",
    "/usr/lib64/faketime/libfaketime.so.1",
  ];
  const library = values.faketime ?? candidates.find(existsSync);
  if (library === undefined || !existsSync(library)) {
    throw new Error("The desktop scenes need libfaketime: install it (apt install libfaketime) or pass --faketime");
  }
  return library;
}

async function startContentServer(served: string): Promise<ChildProcess> {
  const catalog = `${CONTENT_SERVER}/catalog.json`;
  // A server left over from another run would answer in place of this one, with whatever it was serving.
  if (await answers(catalog)) throw new Error(`Something already serves ${catalog}; stop it first`);
  const child = spawn(process.execPath, [join(import.meta.dirname, "..", "content", "serve.ts"), "--root", served], {
    stdio: ["ignore", "ignore", "inherit"],
  });
  for (let attempt = 0; attempt < 50; attempt++) {
    if (child.exitCode !== null) throw new Error(`The content server exited with ${child.exitCode}`);
    if (await answers(catalog)) return child;
    await sleep(200);
  }
  child.kill();
  throw new Error(`The content server did not start on ${catalog}`);
}

async function answers(url: string): Promise<boolean> {
  try {
    return (await fetch(url)).ok;
  } catch {
    return false;
  }
}
