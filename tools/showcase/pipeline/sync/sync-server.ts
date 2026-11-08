/**
 * The sync server for a group whose devices sync (scenes.ts): tools/showcase/sync-server, the real server on a clock
 * standing at the showcase moment, over a data folder emptied first. It listens where the sync shots and the docs show
 * it, on the host's loopback interface, which the emulator reaches through `adb reverse` as it reaches the content
 * server.
 */
import { type ChildProcess, spawn } from "node:child_process";
import { mkdirSync, openSync, rmSync } from "node:fs";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";

export const SYNC_SERVER_PORT = 8080;
export const SYNC_SERVER = `http://127.0.0.1:${SYNC_SERVER_PORT}`;

/** The account the phone and the desktop sign in to, the server's admin, whose web page lists every account. */
export const SYNC_ACCOUNT = { username: "alex", password: "showcase-password" };

/** The other people the server holds accounts for, who show in the admin's list of accounts. */
const OTHER_USERNAMES = ["jamie", "sam"];

/** Starts the server from the `showcase-sync-server` script `:tools:showcase:sync-server:installDist` lays out. */
export async function startSyncServer(launcher: string, workDir: string): Promise<ChildProcess> {
  if (await answers()) throw new Error(`Something already serves ${SYNC_SERVER}; stop it first`);
  rmSync(workDir, { recursive: true, force: true });
  mkdirSync(workDir, { recursive: true });
  const log = join(workDir, "server.log");
  const accounts = [SYNC_ACCOUNT, ...OTHER_USERNAMES.map((username) => ({ username, password: SYNC_ACCOUNT.password }))];
  const server = spawn(
    launcher,
    [`${SHOWCASE_NOW}`, ...accounts.flatMap((account) => [account.username, account.password])],
    {
      env: {
        ...process.env,
        KATARI_SYNC_DATA_DIR: join(workDir, "data"),
        KATARI_SYNC_HOST: "127.0.0.1",
        KATARI_SYNC_PORT: `${SYNC_SERVER_PORT}`,
      },
      stdio: ["ignore", openSync(log, "w"), openSync(log, "a")],
    },
  );
  for (let attempt = 0; attempt < 100; attempt++) {
    if (server.exitCode !== null) throw new Error(`The sync server exited with ${server.exitCode}; see ${log}`);
    if (await answers()) return server;
    await sleep(300);
  }
  server.kill();
  throw new Error(`The sync server did not start on ${SYNC_SERVER}; see ${log}`);
}

async function answers(): Promise<boolean> {
  try {
    await fetch(SYNC_SERVER, { redirect: "manual" });
    return true;
  } catch {
    return false;
  }
}
