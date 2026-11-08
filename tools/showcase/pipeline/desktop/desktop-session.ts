/**
 * The desktop's part of a scene group: a private display, and the desktop app on it set up as the scenes expect, through
 * its first-run guide (the SeedRestore scene).
 */
import { join } from "node:path";
import { timed } from "../timed.ts";
import { DesktopApp } from "./desktop-app.ts";
import { type Display, startDisplay } from "./display.ts";

export interface DesktopOptions {
  /** The `Katari-Dev.AppDir` folder `:desktop:assembleAppDir` lays out. */
  appDir: string;
  agentJar: string;
  extensionApk: string;
  seed: string;
  faketime: string;
  /** A folder of the session's own for the app's profile and the display's state; it is emptied first. */
  workDir: string;
  /** Where the scenes' stills go. */
  out: string;
}

export class DesktopSession {
  private readonly app: DesktopApp;
  private readonly display: Display;

  private constructor(app: DesktopApp, display: Display) {
    this.app = app;
    this.display = display;
  }

  static async open(options: DesktopOptions): Promise<DesktopSession> {
    const display = await startDisplay(join(options.workDir, "display"));
    const app = new DesktopApp({ ...options, workDir: join(options.workDir, "app"), display });
    const session = new DesktopSession(app, display);
    try {
      app.prepare();
      await timed("restoring the seed on the desktop", () =>
        app.runScene("SeedRestore", { seed: options.seed, storage: app.storageFolder }),
      );
    } catch (error) {
      session.close();
      throw error;
    }
    return session;
  }

  run(scene: string): Promise<void> {
    return this.app.runScene(scene);
  }

  close(): void {
    this.display.stop();
  }
}
