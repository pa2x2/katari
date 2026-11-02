import arrowUpward from "../guide/icons/arrow_upward.svg";
import autoStories from "../guide/icons/auto_stories.svg";
import code from "../guide/icons/code.svg";
import description from "../guide/icons/description.svg";
import editNote from "../guide/icons/edit_note.svg";
import extension from "../guide/icons/extension.svg";
import filterList from "../guide/icons/filter_list.svg";
import publicIcon from "../guide/icons/public.svg";
import subtitles from "../guide/icons/subtitles.svg";
import tune from "../guide/icons/tune.svg";
import type { Manual } from "../guide/contents.ts";
import { sdk } from "./sdk.ts";

/** Writing extensions with the Katari SDK, in the order a first extension is built. */
export const developers: Manual = {
  id: "developers",
  name: "Developers",
  titleSuffix: "Katari developer docs",
  base: "developers/",
  badge: `SDK ${sdk.version}`,
  links: [
    { label: "Using Katari", path: "docs/" },
    { label: "Extension compatibility", path: "docs/extension-compatibility/" },
  ],
  groups: [
    {
      title: "Start",
      pages: [
        {
          slug: "",
          title: "How extensions work",
          summary: "What an extension is, what Katari checks before loading one, and SDK versions.",
          icon: extension,
          related: ["developers/project/", "developers/source/", "docs/extension-compatibility/"],
        },
        {
          slug: "project",
          title: "Set up a project",
          summary: "Gradle files, the manifest, and version numbers.",
          icon: code,
          related: ["developers/source/", "developers/store/", "developers/sdk-changelog/"],
        },
      ],
    },
    {
      title: "Write a source",
      pages: [
        {
          slug: "source",
          title: "The source",
          summary: "Lists, search, refreshing details and chapters, and merged entries.",
          icon: editNote,
          related: ["developers/filters/", "developers/media/", "developers/video/"],
        },
        {
          slug: "filters",
          title: "Filters",
          summary: "Search controls, saved presets, dates and options from the site.",
          icon: filterList,
          related: ["developers/source/", "developers/capabilities/"],
        },
        {
          slug: "media",
          title: "Pages and books",
          summary: "What a source hands the reader when a chapter opens.",
          icon: autoStories,
          related: ["developers/video/", "developers/source/"],
        },
        {
          slug: "video",
          title: "Video",
          summary: "Streams, servers, subtitles, audio tracks and skip markers for the player.",
          icon: subtitles,
          related: ["developers/media/", "developers/capabilities/"],
        },
        {
          slug: "capabilities",
          title: "Optional capabilities",
          summary: "Settings, related titles, opening links and more.",
          icon: tune,
          related: ["developers/store/", "developers/source/"],
        },
      ],
    },
    {
      title: "Publish",
      pages: [
        {
          slug: "store",
          title: "Publish an extension store",
          summary: "Sign your extensions and list them where Katari can find them.",
          icon: publicIcon,
          related: ["developers/project/", "docs/extension-stores/"],
        },
      ],
    },
    {
      title: "SDK releases",
      // The changelog, then one guide for each SDK release that older extensions or older Katari can't load, newest first.
      pages: [
        {
          slug: "sdk-changelog",
          title: "SDK changelog",
          summary: "What each SDK release added, changed and removed.",
          icon: description,
          related: ["developers/upgrade/3.0/", "developers/project/"],
        },
        {
          slug: "upgrade/3.0",
          title: "Upgrade to SDK 3.0",
          summary: "Rebuild an SDK 2.x extension: what to change and what to check.",
          icon: arrowUpward,
          related: ["developers/sdk-changelog/", "developers/source/", "developers/media/"],
        },
      ],
    },
  ],
};
