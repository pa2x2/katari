/**
 * The desktop app as the desktop scenes run it: from the AppDir its AppImage is packed from, on a private display,
 * with folders, a session bus, fonts and a browser of its own, so nothing reaches the desktop of the machine running
 * the pipeline. Each scene is a fresh launch with the capture agent (tools/showcase/desktop-capture) loaded through
 * JAVA_TOOL_OPTIONS; the agent runs the scene, writes how it went and closes the app.
 *
 * Every launch starts at the showcase moment, as every phone scene does, through libfaketime: the seed's history and
 * the times shown relative to now read the same as on the phone, and sync orders the two devices' changes the same
 * way whatever day the pipeline runs.
 */
import { spawn } from "node:child_process";
import { chmodSync, copyFileSync, existsSync, mkdirSync, openSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";
import { SHOWCASE_NOW, SHOWCASE_TIME_ZONE } from "../../seed/showcase-clock.ts";
import type { Display } from "./display.ts";

/** The folder name the development build keeps its data under, which the AppDir the pipeline runs is. */
const APP_FOLDER = "katari-dev";
const EXTENSION_PACKAGE = "app.katari.showcase.extension";
/** The captures' size in device pixels: a 1280×800 window at the display's 1.5 scale. */
const WINDOW = { width: 1920, height: 1200 };
// Several times the slowest desktop scene, as scenes.ts allows a phone scene.
const SCENE_TIMEOUT_MS = 15 * 60_000;
/** How long the app may take to close after its scene is done before it is stopped. */
const CLOSE_TIMEOUT_MS = 30_000;
const FONT = "Noto Sans";
/** Where the sync shots and the docs show the computer's name; the app names the device after its host. */
const HOST_NAME = "Desktop";

export interface DesktopAppOptions {
  /** The `Katari-Dev.AppDir` folder `:desktop:assembleAppDir` lays out. */
  appDir: string;
  agentJar: string;
  extensionApk: string;
  /** A folder of the session's own, which the app's profile, logs and settings go in. */
  workDir: string;
  /** Where the scenes' stills go. */
  out: string;
  display: Display;
  /** `libfaketime.so.1`, which the app is started with to run at the showcase moment. */
  faketime: string;
}

export class DesktopApp {
  private readonly options: DesktopAppOptions;
  private readonly home: string;
  /** The folder the first-run guide is told to keep downloads and backups in. */
  readonly storageFolder: string;

  constructor(options: DesktopAppOptions) {
    this.options = options;
    this.home = join(options.workDir, "home");
    this.storageFolder = join(this.home, "Katari");
  }

  /** Lays out the profile: its folders, the showcase extension installed as the app keeps installed apks. */
  prepare(): void {
    rmSync(this.options.workDir, { recursive: true, force: true });
    for (const folder of Object.values(this.folders())) mkdirSync(folder, { recursive: true, mode: 0o700 });
    mkdirSync(this.storageFolder);
    const apks = join(this.folders().XDG_DATA_HOME, APP_FOLDER, "extension-apks");
    mkdirSync(apks, { recursive: true });
    copyFileSync(this.options.extensionApk, join(apks, `${EXTENSION_PACKAGE}.apk`));
    writeFileSync(this.busConfig(), BUS_CONFIG);
    writeFileSync(this.fontConfig(), fontConfig(FONT));
    writeFileSync(this.browser(), `#!/bin/sh\necho "$@" >> '${join(this.options.workDir, "browser.log")}'\n`);
    chmodSync(this.browser(), 0o755);
  }

  /** Runs the desktop scene [scene] (its class under the agent's package) in a fresh launch of the app. */
  async runScene(scene: string, args: Record<string, string> = {}): Promise<void> {
    const result = join(this.options.workDir, "result.txt");
    rmSync(result, { force: true });
    const agentOptions = join(this.options.workDir, "agent.properties");
    writeFileSync(
      agentOptions,
      properties({
        scene,
        captures: this.options.out,
        failures: join(this.options.out, "desktop-failures"),
        result,
        "window.width": `${WINDOW.width}`,
        "window.height": `${WINDOW.height}`,
        ...Object.fromEntries(Object.entries(args).map(([name, value]) => [`argument.${name}`, value])),
      }),
    );
    const log = join(this.options.workDir, `${scene}.log`);
    const output = openSync(log, "w");
    const app = spawn(
      "dbus-run-session",
      ["--config-file", this.busConfig(), "--", join(this.options.appDir, "AppRun")],
      { env: this.environment(agentOptions), stdio: ["ignore", output, output] },
    );
    const exited = new Promise<void>((resolve) => app.once("exit", () => resolve()));
    // Unreferenced, the time limits don't keep the pipeline running once the app has ended.
    const limit = (ms: number) => sleep(ms, undefined, { ref: false });
    try {
      await Promise.race([exited, limit(SCENE_TIMEOUT_MS)]);
      if (existsSync(result) && app.exitCode === null) await Promise.race([exited, limit(CLOSE_TIMEOUT_MS)]);
    } finally {
      if (app.exitCode === null && app.signalCode === null) {
        app.kill();
        await exited;
      }
    }
    const outcome = existsSync(result) ? readFileSync(result, "utf8") : undefined;
    if (outcome !== "OK") {
      copyFileSync(log, join(this.options.out, `desktop-${scene}.log`));
      throw new Error(`desktop ${scene} failed:\n${outcome ?? "the app ended before the scene did; see its log"}`);
    }
  }

  private environment(agentOptions: string): NodeJS.ProcessEnv {
    const inherited = Object.fromEntries(
      Object.entries(process.env).filter(([name]) => !NOT_INHERITED.includes(name)),
    );
    return {
      ...inherited,
      ...this.folders(),
      DISPLAY: this.options.display.name,
      HOSTNAME: HOST_NAME,
      TZ: SHOWCASE_TIME_ZONE,
      LD_PRELOAD: this.options.faketime,
      FAKETIME: `@${new Date(SHOWCASE_NOW).toISOString().slice(0, 19).replace("T", " ")}`,
      // The JVM hangs when libfaketime fakes the monotonic clock as well, as its README warns.
      FAKETIME_DONT_FAKE_MONOTONIC: "1",
      // Turned on by glibc version, this fix reads the deadlines of waits on the monotonic clock as if that clock were
      // faked too; with it unfaked, every timed wait returns at once and the app's idle threads spin.
      FAKETIME_FORCE_MONOTONIC_FIX: "0",
      // The software rasterizer draws the same pixels on every machine; a GPU's antialiasing differs.
      SKIKO_RENDER_API: "SOFTWARE",
      FONTCONFIG_FILE: this.fontConfig(),
      BROWSER: this.browser(),
      // Java takes its home folder from the account, not HOME; the file dialogs open there, and Java keeps its
      // preferences there.
      JAVA_TOOL_OPTIONS: `-Duser.home=${this.home} -javaagent:${this.options.agentJar}=${agentOptions}`,
    };
  }

  private folders() {
    return {
      HOME: this.home,
      XDG_CONFIG_HOME: join(this.home, ".config"),
      XDG_DATA_HOME: join(this.home, ".local/share"),
      XDG_CACHE_HOME: join(this.home, ".cache"),
      XDG_STATE_HOME: join(this.home, ".local/state"),
      // The app's single-instance lock lives here, so it must be the profile's own.
      XDG_RUNTIME_DIR: join(this.options.workDir, "runtime"),
    };
  }

  private busConfig(): string {
    return join(this.options.workDir, "session-bus.conf");
  }

  private fontConfig(): string {
    return join(this.options.workDir, "fonts.conf");
  }

  private browser(): string {
    return join(this.options.workDir, "browser");
  }
}

/**
 * What the pipeline's own session sets that would lead the app to the machine's desktop: its Wayland display and
 * session bus, and the variables by which a desktop or an AppImage the pipeline runs in names itself. AppRun takes an
 * inherited APPDIR as the folder it was mounted from.
 */
const NOT_INHERITED = [
  "WAYLAND_DISPLAY",
  "DBUS_SESSION_BUS_ADDRESS",
  "XDG_CURRENT_DESKTOP",
  "XDG_SESSION_TYPE",
  "APPDIR",
  "APPIMAGE",
  "ARGV0",
  "OWD",
];

/**
 * A session bus that starts no service: one that did would start the machine's desktop portal, which then shows its
 * file dialogs on the machine's own screen. Without a portal, the app falls back to dialogs of its own on the display.
 */
const BUS_CONFIG = `<!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-Bus Bus Configuration 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
<busconfig>
  <type>session</type>
  <listen>unix:tmpdir=/tmp</listen>
  <auth>EXTERNAL</auth>
  <policy context="default">
    <allow send_destination="*" eavesdrop="true"/>
    <allow eavesdrop="true"/>
    <allow own="*"/>
  </policy>
</busconfig>
`;

/** The machine's fonts, with [family] as the sans-serif the app takes as the desktop's interface font. */
function fontConfig(family: string): string {
  return `<?xml version="1.0"?>
<!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd">
<fontconfig>
  <include ignore_missing="yes">/etc/fonts/fonts.conf</include>
  <alias binding="strong">
    <family>sans-serif</family>
    <prefer><family>${family}</family></prefer>
  </alias>
</fontconfig>
`;
}

function properties(values: Record<string, string>): string {
  const escape = (text: string) => text.replace(/\\/g, "\\\\").replace(/\n/g, "\\n");
  return Object.entries(values)
    .map(([key, value]) => `${key}=${escape(value)}\n`)
    .join("");
}
