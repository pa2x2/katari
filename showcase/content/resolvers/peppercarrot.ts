/** Pepper&Carrot episodes, read from the sources tree peppercarrot.com publishes for every episode and language. */
import { readText } from "../download-cache.ts";
import type { LockedChapter, LockedWork } from "../lock.ts";
import type { PepperCarrotWork } from "../source-definitions.ts";
import { pinFile } from "./pinned-file.ts";

const ROOT = "https://www.peppercarrot.com/0_sources/";

interface Credits {
  translation?: string[];
}

export async function resolvePepperCarrot(
  work: PepperCarrotWork,
  refresh: boolean,
): Promise<Omit<LockedWork, "id" | "source" | "cover">> {
  const folders = await episodeFolders();
  const chapters: LockedChapter[] = [];
  const translators = new Set<string>();
  for (let number = work.episodes.first; number <= work.episodes.last; number++) {
    const folder = folders.get(number);
    if (folder === undefined) throw new Error(`Pepper&Carrot has no episode ${number}`);
    const info: { published: string } = JSON.parse(await readText(`${ROOT}${folder}/info.json`));
    const credits: { credits: Credits } = JSON.parse(await readText(`${ROOT}${folder}/lang/${work.language}/info.json`));
    credits.credits.translation
      ?.map(displayName)
      // Episodes credit their original-language text as a translation called "original version".
      .filter((name) => name !== "original version")
      .forEach((name) => translators.add(name));
    const files = [];
    for (const url of await pageUrls(folder, work.language, number)) files.push(await pinFile(url, refresh));
    chapters.push({ number, name: chapterName(work.language, number, folder), date: info.published, files });
  }
  const translation = translators.size > 0 ? `, translated by ${[...translators].join(", ")}` : "";
  return {
    type: "manga",
    title: work.title,
    author: "David Revoy",
    artist: "David Revoy",
    description: work.description,
    genres: work.genres,
    status: "ongoing",
    language: work.language,
    license: { name: "CC BY 4.0", url: "https://creativecommons.org/licenses/by/4.0/" },
    attribution: `Pepper&Carrot by David Revoy, www.peppercarrot.com${translation}`,
    chapters,
  };
}

async function episodeFolders(): Promise<Map<number, string>> {
  const listing = await readText(ROOT);
  return new Map(
    [...listing.matchAll(/href="ep(\d+)_([^"/]+)\/"/g)].map(([, number, name]) => [Number(number), `ep${number}_${name}`]),
  );
}

async function pageUrls(folder: string, language: string, number: number): Promise<string[]> {
  const listing = await readText(`${ROOT}${folder}/low-res/`);
  const episode = String(number).padStart(2, "0");
  const pattern = new RegExp(`href="(${language}_Pepper-and-Carrot_by-David-Revoy_E${episode}P(\\d\\d)\\.jpg)"`, "g");
  const pages = [...listing.matchAll(pattern)]
    .map(([, name, page]) => ({ name: name!, page: Number(page) }))
    // P00 is the title strip the website shows above an episode, not a page.
    .filter(({ page }) => page > 0)
    .sort((a, b) => a.page - b.page);
  if (pages.length === 0) throw new Error(`Pepper&Carrot episode ${number} has no ${language} pages`);
  return pages.map(({ name }) => `${ROOT}${folder}/low-res/${name}`);
}

function chapterName(language: string, number: number, folder: string): string {
  if (language === "ja") return `第${number}話`;
  const title = folder.slice(folder.indexOf("_") + 1).replaceAll("-s-", "'s ").replaceAll("-", " ");
  return `Episode ${number}: ${title}`;
}

function displayName(credit: string): string {
  return credit.split(" <")[0]!;
}
