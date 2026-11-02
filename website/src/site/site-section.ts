import type { ManualId } from "../guide/contents.ts";

/**
 * The part of the site a page belongs to, which the header marks and search lists first: one of the manuals, or the
 * home page, whose content is the features the header's Features link points into.
 */
export type SiteSection = ManualId | "home";
