/**
 * The 1200×630 picture chat apps and social sites show for a link to the site: the tagline beside the three kinds of
 * story, drawn by satori from plain element objects and rasterized by resvg.
 */
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { Resvg } from "@resvg/resvg-js";
import satori from "satori";
import mincho from "@fontsource/shippori-mincho/files/shippori-mincho-latin-700-normal.woff?inline";
import logo from "../site/katari.svg?raw";

/**
 * A capture as a data URL. Read from disk rather than imported: importing a capture marks its full-size original as
 * used, and the build would then ship it beside the resized copies the pages show. The build runs from the site's root.
 */
const capture = (name: string) =>
  `data:image/png;base64,${readFileSync(resolve("src/captures", name)).toString("base64")}`;

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

export async function renderPreviewCard(): Promise<Uint8Array> {
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
        element("div", { fontSize: 26, marginTop: 30, color: "#a9b1c4" }, "Comics, books, and video for Android"),
      ]),
      phone(capture("translation-after.png"), 200, { left: 700, top: 150, transform: "rotate(-8deg)" }),
      phone(capture("book-paper.png"), 200, { left: 975, top: 150, transform: "rotate(8deg)" }),
      phone(capture("library.png"), 230, { left: 825, top: 80 }),
    ],
  );
  const svg = await satori(card as unknown as Parameters<typeof satori>[0], {
    width: WIDTH,
    height: HEIGHT,
    fonts: [{ name: "Shippori Mincho", data: Buffer.from(mincho.split(",")[1]!, "base64"), weight: 700, style: "normal" }],
  });
  return new Uint8Array(new Resvg(svg).render().asPng());
}
