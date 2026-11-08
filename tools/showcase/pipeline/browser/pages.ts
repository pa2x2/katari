/** The browser scenes, by the names scenes.ts gives them: each goes through pages as a person would and saves stills. */
import { SYNC_ACCOUNT, SYNC_SERVER } from "../sync/sync-server.ts";
import type { BrowserPage } from "./browser-session.ts";

export const BROWSER_PAGES: Record<string, (page: BrowserPage) => Promise<void>> = {
  /** The sync server's own web page, signed in as its admin: every account it holds, with their devices. */
  "sync-server": async (page) => {
    await page.open(`${SYNC_SERVER}/sign-in`);
    await page.submit(`(() => {
      const form = document.querySelector("input[name=username]").form;
      form.username.value = ${JSON.stringify(SYNC_ACCOUNT.username)};
      form.password.value = ${JSON.stringify(SYNC_ACCOUNT.password)};
      form.requestSubmit();
    })()`);
    await page.capture("sync-server-web");
  },
};
