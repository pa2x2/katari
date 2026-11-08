/**
 * The desktop's part of a scene group: a private display, and the desktop app on it. A session for a group that doesn't
 * sync sets the app up through its first-run guide with the seed (the SeedRestore scene); a syncing group's first
 * desktop scene goes through the guide itself, joining the phone's account.
 */
import { rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { timed } from "../timed.ts";
import { DesktopApp } from "./desktop-app.ts";
import { type Display, startDisplay } from "./display.ts";

export interface DesktopOptions {
  /** The `Katari.AppDir` folder `:desktop:assembleAppDir -Pdesktop-release` lays out. */
  appDir: string;
  agentJar: string;
  extensionApk: string;
  seed: string;
  faketime: string;
  keyring: string;
  /** A folder of the session's own for the app's profile and the display's state; it is emptied first. */
  workDir: string;
  /** Where the scenes' stills go. */
  out: string;
}

export class DesktopSession {
  private readonly app: DesktopApp;
  private readonly display: Display;
  private readonly workDir: string;

  private constructor(app: DesktopApp, display: Display, workDir: string) {
    this.app = app;
    this.display = display;
    this.workDir = workDir;
  }

  static async open(options: DesktopOptions, seeded: boolean): Promise<DesktopSession> {
    const display = await startDisplay(join(options.workDir, "display"));
    const app = new DesktopApp({ ...options, workDir: join(options.workDir, "app"), display });
    const session = new DesktopSession(app, display, options.workDir);
    try {
      app.prepare();
      if (seeded) {
        await timed("restoring the seed on the desktop", () =>
          app.runScene("SeedRestore", { seed: options.seed, storage: app.storageFolder }),
        );
      }
    } catch (error) {
      session.close();
      throw error;
    }
    return session;
  }

  /** The folder the first-run guide is to keep downloads and backups in. */
  get storageFolder(): string {
    return this.app.storageFolder;
  }

  run(scene: string, args: Record<string, string> = {}): Promise<void> {
    return this.app.runScene(scene, args);
  }

  /**
   * Runs [scene] beside a phone scene, with the clock at [startsAt], where the phone's is. The scene waits for its
   * release (SceneContext.awaitRelease), which comes once [ended] does.
   */
  async runAlongside(scene: string, args: Record<string, string>, startsAt: number, ended: Promise<void>): Promise<void> {
    const release = join(this.workDir, "release");
    rmSync(release, { force: true });
    void ended.then(() => writeFileSync(release, ""));
    await this.app.runScene(scene, { ...args, release }, startsAt);
  }

  close(): void {
    this.display.stop();
  }
}
