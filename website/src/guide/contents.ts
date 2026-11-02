import type { SvgComponent } from "astro/types";
import { developers } from "../developers/contents.ts";
import { docs } from "../docs/contents.ts";

/**
 * The site's two manuals: Docs, for people using Katari, and Developers, for people writing extensions. Each has its
 * own contents, its own label in search results and its own place in the header. A page's file is
 * src/pages/<base><slug>.astro, or <base><slug>/index.astro for a page with pages under it.
 */
export type ManualId = "docs" | "developers";

export interface GuideEntry {
  /** The page's path under its manual's base; empty for the page at the base itself. */
  slug: string;
  title: string;
  /** One line on the manual's index and in the contents' links. */
  summary: string;
  /**
   * The Material Symbol the app shows for the feature, copied from icons/material-symbols. Pages for extension
   * developers describe no screen of the app, so they take the nearest symbol from the same set.
   */
  icon: SvgComponent;
}

export interface GuideGroup {
  title: string;
  pages: GuideEntry[];
  /** Set apart under the other groups, for pages people look for rather than read in turn, such as troubleshooting. */
  boxed?: boolean;
}

export interface ManualLink {
  label: string;
  /** A path on this site. */
  path: string;
}

export interface Manual {
  id: ManualId;
  name: string;
  /** What a page's title ends with, such as "Tracking · Katari docs". */
  titleSuffix: string;
  /** The path its pages live under, ending in a slash. */
  base: string;
  groups: GuideGroup[];
  /** Beside the manual's name above its contents, such as the SDK version the pages describe. */
  badge?: string;
  /** Under the contents: the other parts of the site its readers most often need. */
  links: ManualLink[];
}

export const manuals: Manual[] = [docs, developers];

export function manual(id: ManualId): Manual {
  return manuals.find((candidate) => candidate.id === id)!;
}

/** The page's path on the site, such as `docs/tracking/`. */
export function entryPath(owner: Manual, entry: GuideEntry): string {
  return entry.slug ? `${owner.base}${entry.slug}/` : owner.base;
}

export function guideEntry(
  id: ManualId,
  slug: string,
): { manual: Manual; entry: GuideEntry; group: GuideGroup; previous?: GuideEntry; next?: GuideEntry } {
  const owner = manual(id);
  const pages = owner.groups.flatMap((group) => group.pages);
  const index = pages.findIndex((page) => page.slug === slug);
  if (index < 0) throw new Error(`The ${owner.name} contents have no page ${slug}`);
  const entry = pages[index]!;
  return {
    manual: owner,
    entry,
    group: owner.groups.find((group) => group.pages.includes(entry))!,
    previous: pages[index - 1],
    next: pages[index + 1],
  };
}

/** Every guide page with its path on the site. */
export const guidePaths: { manual: Manual; entry: GuideEntry; path: string }[] = manuals.flatMap((owner) =>
  owner.groups.flatMap((group) => group.pages.map((entry) => ({ manual: owner, entry, path: entryPath(owner, entry) }))),
);
