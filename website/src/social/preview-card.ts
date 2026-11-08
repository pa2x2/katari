/**
 * The 1200×630 picture chat apps and social sites show for a link to the site: the tagline beside the home page's
 * shots, drawn by satori from plain element objects and rasterized by resvg.
 */
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { Resvg } from "@resvg/resvg-js";
import satori from "satori";
import mincho from "@fontsource/shippori-mincho/files/shippori-mincho-latin-700-normal.woff?inline";
import { offeredPlatforms } from "../download/desktop-release.ts";
import logo from "../site/katari.svg?raw";

/**
 * A capture as a data URL. Read from disk rather than imported: importing a capture marks its full-size original as
 * used, and the build would then ship it beside the resized copies the pages show. The build runs from the site's root.
 */
const capturePath = (name: string) => resolve("src/captures", name);
const capture = (name: string) => `data:image/png;base64,${readFileSync(capturePath(name)).toString("base64")}`;

const WIDTH = 1200;
const HEIGHT = 630;

interface Element {
  type: string;
  props: Record<string, unknown> & { style?: Record<string, unknown> };
}

const element = (type: string, style: Record<string, unknown>, children?: unknown, extra: object = {}): Element => ({
  type,
  props: { style, children, ...extra },
});

const phone = (src: string, width: number, style: Record<string, unknown>) =>
  element(
    "div",
    { display: "flex", position: "absolute", padding: 8, borderRadius: 30, background: "#05080f", border: "1px solid #2a3550", ...style },
    element("img", { width, height: (width * 2424) / 1080, borderRadius: 23 }, undefined, { src }),
  );

// The window controls Shot.astro draws in a desktop window's title bar.
const controls = `data:image/svg+xml;base64,${Buffer.from(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 54 12" fill="none" stroke="#f4f1e8" stroke-opacity=".7" stroke-width="1.4"><path d="M1 6.5h9M22.5 2.5h7v7h-7zM43 2.5l7 7m0-7-7 7"/></svg>',
).toString("base64")}`;

const desktopWindow = (src: string, width: number, style: Record<string, unknown>) =>
  element(
    "div",
    {
      display: "flex",
      flexDirection: "column",
      position: "absolute",
      padding: "0 4px 4px",
      borderRadius: 14,
      background: "#05080f",
      border: "1px solid #2a3550",
      ...style,
    },
    [
      element("div", { display: "flex", position: "relative", alignItems: "center", justifyContent: "center", height: 30, fontSize: 15, color: "rgba(244, 241, 232, 0.7)" }, [
        "Katari",
        element("img", { position: "absolute", right: 12, width: 45, height: 10 }, undefined, { src: controls }),
      ]),
      element("img", { width, height: (width * 10) / 16, borderRadius: "0 0 10px 10px" }, undefined, { src }),
    ],
  );

/**
 * The hero's shots: the Linux window behind the comic and book phones once its capture exists, and until then the
 * library on a phone between them, so the card never shows a frame still waiting for its capture.
 */
const shots = () =>
  existsSync(capturePath("sync-desktop.png"))
    ? [
        desktopWindow(capture("sync-desktop.png"), 470, { left: 695, top: 60 }),
        phone(capture("translation-after.png"), 165, { left: 712, top: 232, transform: "rotate(-8deg)" }),
        phone(capture("book-paper.png"), 165, { left: 1000, top: 232, transform: "rotate(8deg)" }),
      ]
    : [
        phone(capture("translation-after.png"), 200, { left: 700, top: 150, transform: "rotate(-8deg)" }),
        phone(capture("book-paper.png"), 200, { left: 975, top: 150, transform: "rotate(8deg)" }),
        phone(capture("library.png"), 230, { left: 825, top: 80 }),
      ];

export async function renderPreviewCard(): Promise<Uint8Array> {
  const platforms = new Intl.ListFormat("en", { type: "conjunction" }).format(await offeredPlatforms());
  const card = element(
    "div",
    {
      display: "flex",
      width: WIDTH,
      height: HEIGHT,
      background: "radial-gradient(circle at 80% 20%, #1b2742, #0b111e 60%)",
      color: "#f4f1e8",
      fontFamily: "Shippori Mincho",
      position: "relative",
      overflow: "hidden",
    },
    [
      element("div", { display: "flex", flexDirection: "column", justifyContent: "center", padding: "0 0 0 80px", width: 620 }, [
        element("div", { display: "flex", alignItems: "center", marginBottom: 44, fontSize: 40 }, [
          element("img", { width: 76, height: 76, marginRight: 22 }, undefined, {
            src: `data:image/svg+xml;base64,${Buffer.from(logo).toString("base64")}`,
          }),
          "Katari",
        ]),
        element("div", { fontSize: 56, lineHeight: 1.12 }, "Every kind of story."),
        element("div", { fontSize: 56, lineHeight: 1.12, color: "#d8895f" }, "One library."),
        element("div", { fontSize: 26, marginTop: 30, color: "#a9b1c4" }, `Comics, books, and video for ${platforms}`),
      ]),
      ...shots(),
    ],
  );
  const svg = await satori(card as unknown as Parameters<typeof satori>[0], {
    width: WIDTH,
    height: HEIGHT,
    fonts: [{ name: "Shippori Mincho", data: Buffer.from(mincho.split(",")[1]!, "base64"), weight: 700, style: "normal" }],
  });
  return new Uint8Array(new Resvg(svg).render().asPng());
}
