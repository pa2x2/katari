/**
 * Offline Translator (github.com/davidventura/offline-translator), the on-device engine the translation scenes use, so
 * they depend on no live service. Its Japanese to English models are pushed to the external storage it reads them from,
 * and its LibreTranslate-compatible server, which Katari calls on 127.0.0.1:5000, is switched on in its settings.
 *
 * Its only build is for arm64; the x86_64 emulator runs it through its native bridge.
 */
import { mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { gunzipSync } from "node:zlib";
import { ensure } from "../../content/download-cache.ts";
import type { Device } from "./adb.ts";
import { writePreferences } from "./shared-preferences.ts";

export const OFFLINE_TRANSLATOR_ID = "dev.davidv.translator";

const APK = {
  url: "https://github.com/davidventura/offline-translator/releases/download/v0.8.5/translator-arm64-0.8.5.apk",
  sha256: "e44fc1bf41a284692230a2666ba779808b4e1b810de8a206d933c8832d038000",
};

const MODELS = "https://offline-translator.davidv.dev/translation/1/models/ja-en/cjk_retrain_base-memory_NLRJLD_pQFyrvgKtbie2nA/exported";

/** The files of its `translate-ja-en` pack, gzipped as served, with where it installs each. */
const JAPANESE_TO_ENGLISH = [
  {
    url: `${MODELS}/model.jaen.intgemm.alphas.bin.gz`,
    sha256: "ae56ffbb5556d8e4240b2f208a7c7a2449a4b627ac9d673981ed29eaadaab79d",
    installPath: "bin/model.jaen.intgemm.alphas.bin",
  },
  {
    url: `${MODELS}/lex.50.50.jaen.s2t.bin.gz`,
    sha256: "438152f5ccd982edb43e88ef51305e3ae7c7b66ee5c20a8fa425e9f1822f9b9b",
    installPath: "bin/lex.50.50.jaen.s2t.bin",
  },
  {
    url: `${MODELS}/vocab.jaen.spm.gz`,
    sha256: "12d693f5055525d5cc1e133c8c1b8ed787c77b9bb797400d9a14382ac69c1236",
    installPath: "bin/vocab.jaen.spm",
  },
];

const EXTERNAL_DIRECTORY = `/sdcard/Documents/${OFFLINE_TRANSLATOR_ID}`;

export async function installOfflineTranslator(device: Device): Promise<void> {
  const directory = mkdtempSync(join(tmpdir(), "katari-translator-"));
  try {
    // adb installs only files named like packages, and the download cache names its files by hash.
    const apk = join(directory, "offline-translator.apk");
    symlinkSync(await ensure(APK.url, APK.sha256), apk);
    device.adb("install", "-r", apk);
    for (const file of JAPANESE_TO_ENGLISH) {
      const local = join(directory, file.installPath.replaceAll("/", "_"));
      writeFileSync(local, gunzipSync(readFileSync(await ensure(file.url, file.sha256))));
      const remote = `${EXTERNAL_DIRECTORY}/${file.installPath}`;
      device.shell(`mkdir -p ${remote.slice(0, remote.lastIndexOf("/"))}`);
      device.push(local, remote);
    }
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
  // Reading models outside its own storage needs all-files access, which it otherwise asks for on screen.
  device.shell(`appops set ${OFFLINE_TRANSLATOR_ID} MANAGE_EXTERNAL_STORAGE allow`);
  writePreferences(device, OFFLINE_TRANSLATOR_ID, "app_settings.xml", {
    use_external_storage: true,
    http_server_enabled: true,
  });
}

/** Its server runs while the app's process does, which launching it starts. */
export function startOfflineTranslator(device: Device): void {
  device.shell(`monkey -p ${OFFLINE_TRANSLATOR_ID} -c android.intent.category.LAUNCHER 1`);
}
