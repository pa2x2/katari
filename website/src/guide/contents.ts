import type { SvgComponent } from "astro/types";
import type { Platform } from "./platforms.ts";
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
   * The Material Symbol the app shows for the feature, copied from core/icons/material-symbols. Pages for extension
   * developers describe no screen of the app, so they take the nearest symbol from the same set.
   */
  icon: SvgComponent;
  /** The one app the page's feature is in; unset when both apps have it. */
  platform?: Platform;
  /** Paths of the pages worth reading next, picked by hand and shown at the page's end; any manual's pages may appear. */
  related: string[];
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
  /** What a page's title ends with, such as "Profiles · Katari docs". */
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

/** The page's path on the site, such as `docs/profiles/`. */
export function entryPath(owner: Manual, entry: GuideEntry): string {
  return entry.slug ? `${owner.base}${entry.slug}/` : owner.base;
}

export function guideEntry(id: ManualId, slug: string): { manual: Manual; entry: GuideEntry; group: GuideGroup } {
  const owner = manual(id);
  const group = owner.groups.find((candidate) => candidate.pages.some((page) => page.slug === slug));
  if (!group) throw new Error(`The ${owner.name} contents have no page ${slug}`);
  return { manual: owner, entry: group.pages.find((page) => page.slug === slug)!, group };
}

/** Every guide page with its path on the site. */
export const guidePaths: { manual: Manual; entry: GuideEntry; path: string }[] = manuals.flatMap((owner) =>
  owner.groups.flatMap((group) => group.pages.map((entry) => ({ manual: owner, entry, path: entryPath(owner, entry) }))),
);

/** The pages an entry lists as related, failing the build on a path that names no guide page. */
export function relatedPages(entry: GuideEntry): { entry: GuideEntry; path: string }[] {
  return entry.related.map((path) => {
    const page = guidePaths.find((candidate) => candidate.path === path);
    if (!page) throw new Error(`${entry.title} lists ${path} as related, which is no guide page`);
    return page;
  });
}
