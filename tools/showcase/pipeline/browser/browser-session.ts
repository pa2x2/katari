/**
 * Headless Chrome for a group's browser scenes (pages.ts), driven over the DevTools protocol. Its one page is the size
 * of the site's browser frame, 16:10, at the scale the desktop shots are taken at, so a still matches a Linux window's.
 */
import { type ChildProcess, spawn } from "node:child_process";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { DevTools } from "./devtools.ts";
import { BROWSER_PAGES } from "./pages.ts";

const VIEWPORT = { width: 1280, height: 800, deviceScaleFactor: 1.5 };
const START_TIMEOUT_MS = 30_000;

export class BrowserPage {
  private readonly devTools: DevTools;
  private readonly sessionId: string;
  private readonly out: string;

  constructor(devTools: DevTools, sessionId: string, out: string) {
    this.devTools = devTools;
    this.sessionId = sessionId;
    this.out = out;
  }

  /** Opens [url] and waits for it to load. */
  async open(url: string): Promise<void> {
    const loaded = this.devTools.event("Page.loadEventFired", this.sessionId);
    await this.send("Page.navigate", { url });
    await loaded;
  }

  /** Runs [script] in the page, which leads to another page, and waits for that one to load. */
  async submit(script: string): Promise<void> {
    const loaded = this.devTools.event("Page.loadEventFired", this.sessionId);
    await this.evaluate(script);
    await loaded;
  }

  /** Runs [expression] in the page and returns its value, once a promise it gives has settled. */
  async evaluate(expression: string): Promise<unknown> {
    const answer = await this.send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
    const thrown = answer.exceptionDetails as { exception?: { description?: string } } | undefined;
    if (thrown) throw new Error(`The page threw: ${thrown.exception?.description ?? expression}`);
    return (answer.result as { value?: unknown }).value;
  }

  /** Saves the page as it shows now to `[name].png` among the run's captures, once its fonts are in. */
  async capture(name: string): Promise<void> {
    await this.evaluate("document.fonts.ready.then(() => true)");
    const shot = await this.send("Page.captureScreenshot", { format: "png" });
    writeFileSync(join(this.out, `${name}.png`), Buffer.from(shot.data as string, "base64"));
  }

  private send(method: string, params: Record<string, unknown>): Promise<Record<string, unknown>> {
    return this.devTools.send(method, params, this.sessionId);
  }
}

export class BrowserSession {
  private readonly chrome: ChildProcess;
  private readonly devTools: DevTools;
  private readonly page: BrowserPage;

  private constructor(chrome: ChildProcess, devTools: DevTools, page: BrowserPage) {
    this.chrome = chrome;
    this.devTools = devTools;
    this.page = page;
  }

  /** Starts [chrome] on a profile of its own in [workDir], emptied first. */
  static async open(chrome: string, workDir: string, out: string): Promise<BrowserSession> {
    rmSync(workDir, { recursive: true, force: true });
    mkdirSync(workDir, { recursive: true });
    const process = spawn(
      chrome,
      [
        "--headless",
        "--remote-debugging-port=0",
        `--user-data-dir=${join(workDir, "profile")}`,
        "--no-first-run",
        "--no-default-browser-check",
        "--hide-scrollbars",
        "--force-color-profile=srgb",
        "--lang=en-US",
        "about:blank",
      ],
      { stdio: ["ignore", "ignore", "pipe"] },
    );
    try {
      const devTools = await DevTools.connect(await devToolsAddress(process));
      const { targetId } = await devTools.send("Target.createTarget", { url: "about:blank" });
      const { sessionId } = await devTools.send("Target.attachToTarget", { targetId, flatten: true });
      const session = sessionId as string;
      await devTools.send("Page.enable", {}, session);
      await devTools.send("Emulation.setDeviceMetricsOverride", { ...VIEWPORT, mobile: false }, session);
      return new BrowserSession(process, devTools, new BrowserPage(devTools, session, out));
    } catch (error) {
      process.kill();
      throw error;
    }
  }

  run(page: string): Promise<void> {
    const scene = BROWSER_PAGES[page];
    if (scene === undefined) throw new Error(`browser/pages.ts has no page called ${page}`);
    return scene(this.page);
  }

  close(): void {
    this.devTools.close();
    this.chrome.kill();
  }
}

/** The address Chrome prints once it listens for the DevTools protocol, on the port it picked. */
function devToolsAddress(chrome: ChildProcess): Promise<string> {
  return new Promise((resolve, reject) => {
    let printed = "";
    const timer = setTimeout(() => reject(new Error(`Chrome didn't start:\n${printed}`)), START_TIMEOUT_MS);
    chrome.once("exit", (code) => reject(new Error(`Chrome exited with ${code}:\n${printed}`)));
    chrome.stderr!.on("data", (chunk: Buffer) => {
      printed += chunk.toString();
      const address = printed.match(/DevTools listening on (ws:\/\/\S+)/)?.[1];
      if (address !== undefined) {
        clearTimeout(timer);
        resolve(address);
      }
    });
  });
}
