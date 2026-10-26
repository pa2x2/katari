import changelog from "../../../../SDK-CHANGELOG.md?raw";
import { inlineMarkdown } from "../../changelog/inline-markdown.ts";
import { type Release, releases } from "../../changelog/release-notes.ts";
import { pathOnSite, sitePath } from "../../site/links.ts";
import { guidePages } from "../contents.ts";

/** Every SDK release in SDK-CHANGELOG.md, newest first. */
export const sdkReleases: Release[] = releases(changelog);

/**
 * One line of SDK-CHANGELOG.md as HTML. The file is read on GitHub too, so it links pages of this site by their full
 * address; those links stay on the site, and the build fails on one that names no page of the guide, so a renamed or
 * misspelled guide can't publish a dead link.
 */
export function sdkChangelogHtml(line: string): string {
  return inlineMarkdown(line, (url) => {
    const path = pathOnSite(url);
    if (path === undefined) return `href="${url}" target="_blank" rel="noopener"`;
    const slug = /^docs\/(.+?)\/(?:#.*)?$/.exec(path)?.[1];
    if (!guidePages.some((page) => page.slug === slug)) {
      throw new Error(`SDK-CHANGELOG.md links to ${url}, which is no page of the guide`);
    }
    return `href="${sitePath(path)}"`;
  });
}
