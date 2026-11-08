/**
 * A private X display for the desktop app: Xvfb where it is installed, as on CI, or else a rootful Xwayland inside a
 * headless KWin. Neither runs a window manager, so a window on it is its content alone and the site draws the frame.
 * Both keep their resources while no client is connected (-noreset), and the app reads its scale from them when it
 * starts: Xft.dpi 144 is the 1.5 scale the captures are taken at.
 */
import { type ChildProcess, spawn, spawnSync } from "node:child_process";
import { existsSync, mkdirSync } from "node:fs";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";

/** Room for the 1920×1200 main window at the top left and the picture-in-picture window the app puts below it. */
const SCREEN = { width: 2560, height: 1600 };
const XFT_DPI = 144;
const FIRST_DISPLAY = 40;
const START_TIMEOUT_MS = 30_000;

export interface Display {
  /** Such as `:40`. */
  name: string;
  stop(): void;
}

/** Starts a display on the first number no X server holds; [workDir] keeps the headless KWin's own state. */
export async function startDisplay(workDir: string): Promise<Display> {
  const number = freeDisplayNumber();
  const name = `:${number}`;
  const processes = hasCommand("Xvfb") ? [startXvfb(name)] : await startXwaylandInKwin(name, number, workDir);
  const stop = () => processes.toReversed().forEach((child) => child.kill());
  try {
    await awaitSocket(number, processes);
    const resources = spawnSync("xrdb", ["-merge"], {
      env: { ...process.env, DISPLAY: name },
      input: `Xft.dpi: ${XFT_DPI}\n`,
    });
    if (resources.status !== 0) throw new Error(`xrdb could not set the display's scale: ${resources.stderr}`);
  } catch (error) {
    stop();
    throw error;
  }
  return { name, stop };
}

function startXvfb(name: string): ChildProcess {
  return spawn("Xvfb", [name, "-screen", "0", `${SCREEN.width}x${SCREEN.height}x24`, "-noreset", "-nolisten", "tcp"], {
    stdio: "ignore",
  });
}

/**
 * KWin gets folders of its own for its configuration, data and runtime files: started with the user's, it writes its
 * virtual screen into the user's own KWin settings.
 */
async function startXwaylandInKwin(name: string, number: number, workDir: string): Promise<ChildProcess[]> {
  const home = join(workDir, `kwin-${number}`);
  const folders = Object.fromEntries(
    ["XDG_CONFIG_HOME", "XDG_DATA_HOME", "XDG_CACHE_HOME", "XDG_STATE_HOME", "XDG_RUNTIME_DIR"].map((variable) => {
      const folder = join(home, variable.toLowerCase());
      mkdirSync(folder, { recursive: true, mode: 0o700 });
      return [variable, folder];
    }),
  );
  const { WAYLAND_DISPLAY: _, DISPLAY: __, DBUS_SESSION_BUS_ADDRESS: ___, ...environment } = process.env;
  const socket = `katari-showcase-${number}`;
  const kwin = spawn(
    "kwin_wayland",
    ["--virtual", "--width", `${SCREEN.width}`, "--height", `${SCREEN.height}`, "--socket", socket],
    { env: { ...environment, ...folders }, stdio: "ignore" },
  );
  const socketPath = join(folders.XDG_RUNTIME_DIR!, socket);
  for (let waited = 0; !existsSync(socketPath); waited += 200) {
    if (kwin.exitCode !== null || waited > START_TIMEOUT_MS) {
      kwin.kill();
      throw new Error(`KWin did not start its Wayland socket ${socketPath}`);
    }
    await sleep(200);
  }
  const xwayland = spawn("Xwayland", [name, "-noreset", "-geometry", `${SCREEN.width}x${SCREEN.height}`], {
    env: { ...environment, ...folders, WAYLAND_DISPLAY: socket },
    stdio: "ignore",
  });
  return [kwin, xwayland];
}

async function awaitSocket(number: number, processes: ChildProcess[]): Promise<void> {
  for (let waited = 0; !existsSync(`/tmp/.X11-unix/X${number}`); waited += 200) {
    const exited = processes.find((child) => child.exitCode !== null);
    if (exited !== undefined) throw new Error(`The display server exited with ${exited.exitCode}`);
    if (waited > START_TIMEOUT_MS) throw new Error(`Display :${number} did not start`);
    await sleep(200);
  }
}

function freeDisplayNumber(): number {
  for (let number = FIRST_DISPLAY; number < FIRST_DISPLAY + 100; number++) {
    if (!existsSync(`/tmp/.X11-unix/X${number}`) && !existsSync(`/tmp/.X${number}-lock`)) return number;
  }
  throw new Error("No free X display number");
}

function hasCommand(command: string): boolean {
  return spawnSync("sh", ["-c", `command -v ${command}`]).status === 0;
}
