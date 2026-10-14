import { fileURLToPath } from "node:url";
import { defineConfig } from "astro/config";
import { usedFontSlices } from "./plugins/used-font-slices.mjs";

export default defineConfig({
  site: "https://pa2x2.github.io",
  base: "/katari",
  // Compression drops the space where a line of text wraps just before a tag, gluing "under" to a bold "Content types".
  compressHTML: false,
  vite: {
    plugins: [usedFontSlices(fileURLToPath(new URL("./src", import.meta.url)))],
  },
});
