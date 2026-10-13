import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://pa2x2.github.io",
  base: "/katari",
  // Compression drops the space where a line of text wraps just before a tag, gluing "under" to a bold "Content types".
  compressHTML: false,
});
