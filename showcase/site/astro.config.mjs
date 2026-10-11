import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://pa2x2.github.io",
  base: "/katari",
  // The credits page reads the content lock beside the site.
  vite: { server: { fs: { allow: [".."] } } },
});
